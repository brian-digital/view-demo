/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.repositories

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.mongodb.scala.model.{Filters, Indexes}
import org.mongodb.scala.{MongoWriteException, ObservableFuture}
import org.scalatest.OptionValues
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.test.{Helpers, Injecting}
import uk.gov.hmrc.blockandsignalsgdsstore.adapters.SignalProcessorEventAdapters
import uk.gov.hmrc.blockandsignalsgdsstore.config.EventStoreConfig
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.*
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.{SearchRequest, SearchResult}
import uk.gov.hmrc.blockandsignalsgdsstore.test.Generators
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.*
import java.time.temporal.{ChronoUnit, TemporalUnit}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration, MILLISECONDS}

class EventDocumentRepositoryISpec
    extends AnyWordSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[EventDocument]
    with IntegrationPatience
    with OptionValues
    with ScalaFutures
    with GuiceOneAppPerSuite
    with Eventually
    with Injecting
    with Generators {

  implicit val executionContext: ExecutionContext = Helpers.stubControllerComponents().executionContext

  implicit val clock: Clock = Clock.systemUTC()

  val eventStoreConfig: EventStoreConfig = inject[EventStoreConfig].copy(eventTtl = 3.seconds)

  val expectedTtl: FiniteDuration = eventStoreConfig.eventTtl

  override val repository: EventDocumentRepository = new EventDocumentRepository(mongoComponent, eventStoreConfig)

  private def eventToDocument(event: Event): EventDocument =
    SignalProcessorEventAdapters.eventToEventDocument(event).toOption.get

  "EventDocumentRepository mongo repository" should {
    "configure TTL correctly" in {
      val indexModel = repository.indexes.find(m => m.getKeys == Indexes.ascending("storedAt")).value
      indexModel.getOptions.getName                          shouldBe "storedAtIndex"
      indexModel.getOptions.getExpireAfter(expectedTtl.unit) shouldBe expectedTtl.length
    }

    "ensure only EventDocument with unique eventId value can be added" in {
      val event = eventGen.sample.get
      val document = eventToDocument(event)

      val result = intercept[MongoWriteException] {
        await(for {
          _      <- repository.insert(document)
          result <- repository.insert(document)
        } yield result)
      }

      result.getCode shouldBe 11000
      val expectedMessage =
        s"""E11000 duplicate key error collection: $databaseName.$collectionName index: ${indexes.head.getOptions.getName} dup key: { ${indexes.head.getKeys.toBsonDocument.getFirstKey}: "${event.eventId}" }"""
      result.getError.getMessage shouldBe expectedMessage
    }

    "insert an EventDocument only" in {
      val now = Instant.now()
      val instantFrom = now.minusSeconds(10)
      val instantTo = now

      val event: Event = credentialConcernEventGen(instantFrom, instantTo).sample.get
      val eventDocument = eventToDocument(event)

      repository.insert(eventDocument).futureValue

      val results = repository.collection.find(Filters.empty).toFuture().futureValue
      results.length mustBe 1
      results.head mustBe eventDocument.copy(storedAt = eventDocument.storedAt.truncatedTo(ChronoUnit.MILLIS))
    }

    "insert and search" when {

      "eventType is AccountIntervention, initiatingEntity is Analyst" in {
        given Materializer = app.materializer

        val event: Event = interventionEventGen(isAnalyst = true).sample.get
        val eventDocument = eventToDocument(event)

        repository.insert(eventDocument).futureValue

        val searchRequest = SearchRequest(
          dateFrom   = eventDocument.timeOfInterest.atZone(ZoneId.of("UTC")).minusMinutes(1).toLocalDate,
          dateTo     = eventDocument.timeOfInterest.atZone(ZoneId.of("UTC")).plusMinutes(1).toLocalDate,
          credIds    = Some(Seq(event.credId.get)),
          subjectIds = Some(Seq(event.subjectId.get)),
          eventType  = Some(EventType.AccountIntervention)
        )

        val source = repository.find(searchRequest)
        val result = source.runWith(Sink.fold(Seq.empty[SearchResult])(_ :+ _)).futureValue

        result.length mustBe 1

        result must contain only SearchResult(
          credId           = eventDocument.event.credId,
          subjectId        = eventDocument.event.subjectId,
          timeOfInterest   = eventDocument.timeOfInterest,
          eventType        = event.eventType,
          action           = eventDocument.action,
          reason           = None,
          rationale        = None,
          initiatingEntity = event.eventData.asInstanceOf[AccountInterventionEventData].initiatingEntity,
          credentialType   = None,
          reasonAdmin      = None,
          reasonUser       = None,
          emailAddress     = None,
          interventionCode = None
        )
      }

      "eventType AccountIntervention, initiatingEntity is not Analyst" in {
        given Materializer = app.materializer

        val event: Event = interventionEventGen(isAnalyst = false).sample.get
        val eventDocument = eventToDocument(event)

        repository.insert(eventDocument).futureValue

        val searchRequest = SearchRequest(
          dateFrom   = eventDocument.timeOfInterest.atZone(ZoneId.of("UTC")).minusMinutes(1).toLocalDate,
          dateTo     = eventDocument.timeOfInterest.atZone(ZoneId.of("UTC")).plusMinutes(1).toLocalDate,
          credIds    = Some(Seq(event.credId.get)),
          subjectIds = Some(Seq(event.subjectId.get)),
          eventType  = Some(EventType.AccountIntervention)
        )

        val source = repository.find(searchRequest)
        val result = source.runWith(Sink.fold(Seq.empty[SearchResult])(_ :+ _)).futureValue

        result.length mustBe 0
      }

      "eventType is AccountConcern, with dateFrom and dateTo fields" in {
        given Materializer = app.materializer

        val searchRequest = validSearchRequestGen.sample.get.copy(
          credIds    = None,
          subjectIds = None,
          eventType  = Some(EventType.AccountConcern)
        )
        val instantFrom = searchRequest.dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC)
        val instantTo = searchRequest.dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1)

        val eventOne = eventToDocument(concernEventGen(instantFrom, instantTo).sample.get)
        val eventTwo = eventToDocument(concernEventGen(instantFrom, instantTo).sample.get)
        val eventThree = eventToDocument(concernEventGen(instantFrom, instantTo).sample.get)
        val eventFour = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = false).sample.get)

        repository.insert(eventOne).futureValue
        repository.insert(eventTwo).futureValue
        repository.insert(eventThree).futureValue
        repository.insert(eventFour).futureValue

        val source = repository.find(searchRequest)
        val result = source.runWith(Sink.fold(Seq.empty[SearchResult])(_ :+ _)).futureValue

        result must contain only (
          SearchResult(
            eventOne.event.credId,
            eventOne.event.subjectId,
            eventOne.timeOfInterest,
            eventType        = eventOne.event.eventType,
            action           = None,
            reason           = eventOne.event.eventData.asInstanceOf[AccountConcernEventData].reason,
            rationale        = eventOne.event.eventData.asInstanceOf[AccountConcernEventData].rationale,
            initiatingEntity = eventOne.event.eventData.asInstanceOf[AccountConcernEventData].initiatingEntity,
            credentialType   = None,
            reasonAdmin      = None,
            reasonUser       = None,
            emailAddress     = None,
            interventionCode = None
          ),
          SearchResult(
            eventTwo.event.credId,
            eventTwo.event.subjectId,
            eventTwo.timeOfInterest,
            eventType        = eventTwo.event.eventType,
            action           = None,
            reason           = eventTwo.event.eventData.asInstanceOf[AccountConcernEventData].reason,
            rationale        = eventTwo.event.eventData.asInstanceOf[AccountConcernEventData].rationale,
            initiatingEntity = eventTwo.event.eventData.asInstanceOf[AccountConcernEventData].initiatingEntity,
            credentialType   = None,
            reasonAdmin      = None,
            reasonUser       = None,
            emailAddress     = None,
            interventionCode = None
          ),
          SearchResult(
            eventThree.event.credId,
            eventThree.event.subjectId,
            eventThree.timeOfInterest,
            eventType        = eventThree.event.eventType,
            action           = None,
            reason           = eventThree.event.eventData.asInstanceOf[AccountConcernEventData].reason,
            rationale        = eventThree.event.eventData.asInstanceOf[AccountConcernEventData].rationale,
            initiatingEntity = eventThree.event.eventData.asInstanceOf[AccountConcernEventData].initiatingEntity,
            credentialType   = None,
            reasonAdmin      = None,
            reasonUser       = None,
            emailAddress     = None,
            interventionCode = None
          )
        )
      }

      "eventType is AccountIntervention, with dateFrom and dateTo fields" in {

        given Materializer = app.materializer

        val searchRequest: SearchRequest =
          validSearchRequestGen.sample.get.copy(credIds = None, subjectIds = None, eventType = Some(EventType.AccountIntervention))

        val instantFrom = searchRequest.dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC)
        val instantTo = searchRequest.dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1)

        val eventOne: EventDocument = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = true).sample.get)
        val eventTwo: EventDocument = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = false).sample.get)
        val eventThree: EventDocument = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = false).sample.get)
        val eventFour: EventDocument = eventToDocument(concernEventGen(instantFrom, instantTo).sample.get)

        repository.insert(eventOne).futureValue
        repository.insert(eventTwo).futureValue
        repository.insert(eventThree).futureValue
        repository.insert(eventFour).futureValue

        val source = repository.find(searchRequest)
        val result: Seq[SearchResult] = source.runWith(Sink.fold(Seq.empty[SearchResult])(_ :+ _)).futureValue

        result.length mustBe 1

        result must contain only SearchResult(
          eventOne.event.credId,
          eventOne.event.subjectId,
          eventOne.timeOfInterest,
          eventType        = eventOne.event.eventType,
          action           = eventOne.action,
          reason           = None,
          rationale        = None,
          initiatingEntity = eventOne.event.eventData.asInstanceOf[AccountInterventionEventData].initiatingEntity,
          credentialType   = None,
          reasonAdmin      = None,
          reasonUser       = None,
          emailAddress     = None,
          interventionCode = None
        )

      }

      "eventType is AccountIntervention, with dateFrom, dateTo and single subjectId fields" in {
        given Materializer = app.materializer

        val subjectId = subjectIdGen.sample
        val searchRequest = validSearchRequestGen.sample.get.copy(
          credIds    = None,
          subjectIds = Some(Seq(subjectId.get)),
          eventType  = Some(EventType.AccountIntervention)
        )
        val instantFrom = searchRequest.dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC)
        val instantTo = searchRequest.dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1)

        val eventOne = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = true).sample.get.copy(subjectId = subjectId))
        val eventTwo = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = false).sample.get.copy(subjectId = subjectId))
        val eventThree = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = false).sample.get.copy(subjectId = subjectId))

        repository.insert(eventOne).futureValue
        repository.insert(eventTwo).futureValue
        repository.insert(eventThree).futureValue

        val source = repository.find(searchRequest)
        val result = source.runWith(Sink.fold(Seq.empty[SearchResult])(_ :+ _)).futureValue

        result.length mustBe 1

        result must contain only (
          SearchResult(
            eventOne.event.credId,
            eventOne.event.subjectId,
            eventOne.timeOfInterest,
            eventType        = eventOne.event.eventType,
            action           = eventOne.action,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventOne.event.eventData.asInstanceOf[AccountInterventionEventData].initiatingEntity,
            credentialType   = None,
            reasonAdmin      = None,
            reasonUser       = None,
            emailAddress     = None,
            interventionCode = None
          )
        )
      }
    }

    "return correct search results" when {
      "the filters are dateFrom, dateTo and multiple subjectId fields" in {
        given Materializer = app.materializer

        val subjectId1 = subjectIdGen.sample
        val subjectId2 = subjectIdGen.sample
        val subjectId3 = subjectIdGen.sample

        val searchRequest = validSearchRequestGen.sample.get.copy(
          credIds    = None,
          subjectIds = Some(Seq(subjectId1.get, subjectId2.get, subjectId3.get)),
          eventType  = None
        )
        val instantFrom = searchRequest.dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC)
        val instantTo = searchRequest.dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1)

        val eventOne = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = true).sample.get.copy(subjectId = subjectId1))
        val eventTwo = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = true).sample.get.copy(subjectId = subjectId2))
        val eventThree = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = true).sample.get.copy(subjectId = subjectId3))

        repository.insert(eventOne).futureValue
        repository.insert(eventTwo).futureValue
        repository.insert(eventThree).futureValue

        val source = repository.find(searchRequest)
        val result = source.runWith(Sink.fold(Seq.empty[SearchResult])(_ :+ _)).futureValue

        result must contain only (
          SearchResult(
            eventOne.event.credId,
            eventOne.event.subjectId,
            eventOne.timeOfInterest,
            eventType        = eventOne.event.eventType,
            action           = eventOne.action,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventOne.event.eventData.asInstanceOf[AccountInterventionEventData].initiatingEntity,
            credentialType   = None,
            reasonAdmin      = None,
            reasonUser       = None,
            emailAddress     = None,
            interventionCode = None
          ),
          SearchResult(
            eventTwo.event.credId,
            eventTwo.event.subjectId,
            eventTwo.timeOfInterest,
            eventType        = eventTwo.event.eventType,
            action           = eventTwo.action,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventTwo.event.eventData.asInstanceOf[AccountInterventionEventData].initiatingEntity,
            credentialType   = None,
            reasonAdmin      = None,
            reasonUser       = None,
            emailAddress     = None,
            interventionCode = None
          ),
          SearchResult(
            eventThree.event.credId,
            eventThree.event.subjectId,
            eventThree.timeOfInterest,
            eventType        = eventThree.event.eventType,
            action           = eventThree.action,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventThree.event.eventData.asInstanceOf[AccountInterventionEventData].initiatingEntity,
            credentialType   = None,
            reasonAdmin      = None,
            reasonUser       = None,
            emailAddress     = None,
            interventionCode = None
          )
        )

      }

      "the filters are dateFrom, dateTo and single credId fields" in {
        given Materializer = app.materializer

        val credId = credIdGen.sample

        val searchRequest = validSearchRequestGen.sample.get.copy(
          credIds    = Some(Seq(credId.get)),
          subjectIds = None,
          eventType  = None
        )
        val instantFrom = searchRequest.dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC)
        val instantTo = searchRequest.dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1)

        val eventOne = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = true).sample.get.copy(credId = credId))
        val eventTwo = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = true).sample.get.copy(credId = credId))
        val eventThree = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = true).sample.get.copy(credId = credId))

        repository.insert(eventOne).futureValue
        repository.insert(eventTwo).futureValue
        repository.insert(eventThree).futureValue

        val source = repository.find(searchRequest)
        val result = source.runWith(Sink.fold(Seq.empty[SearchResult])(_ :+ _)).futureValue

        result must contain only (
          SearchResult(
            eventOne.event.credId,
            eventOne.event.subjectId,
            eventOne.timeOfInterest,
            eventType        = eventOne.event.eventType,
            action           = eventOne.action,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventOne.event.eventData.asInstanceOf[AccountInterventionEventData].initiatingEntity,
            credentialType   = None,
            reasonAdmin      = None,
            reasonUser       = None,
            emailAddress     = None,
            interventionCode = None
          ),
          SearchResult(
            eventTwo.event.credId,
            eventTwo.event.subjectId,
            eventTwo.timeOfInterest,
            eventType        = eventTwo.event.eventType,
            action           = eventTwo.action,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventTwo.event.eventData.asInstanceOf[AccountInterventionEventData].initiatingEntity,
            credentialType   = None,
            reasonAdmin      = None,
            reasonUser       = None,
            emailAddress     = None,
            interventionCode = None
          ),
          SearchResult(
            eventThree.event.credId,
            eventThree.event.subjectId,
            eventThree.timeOfInterest,
            eventType        = eventThree.event.eventType,
            action           = eventThree.action,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventThree.event.eventData.asInstanceOf[AccountInterventionEventData].initiatingEntity,
            credentialType   = None,
            reasonAdmin      = None,
            reasonUser       = None,
            emailAddress     = None,
            interventionCode = None
          )
        )
      }

      "the filters are dateFrom, dateTo and multiple credId fields" in {
        given Materializer = app.materializer

        val credId1 = credIdGen.sample
        val credId2 = credIdGen.sample
        val credId3 = credIdGen.sample

        val searchRequest = validSearchRequestGen.sample.get.copy(
          credIds    = Some(Seq(credId1.get, credId2.get, credId3.get)),
          subjectIds = None,
          eventType  = None
        )
        val instantFrom = searchRequest.dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC)
        val instantTo = searchRequest.dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1)

        val eventOne = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = true).sample.get.copy(credId = credId1))
        val eventTwo = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = true).sample.get.copy(credId = credId2))
        val eventThree = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = true).sample.get.copy(credId = credId3))

        repository.insert(eventOne).futureValue
        repository.insert(eventTwo).futureValue
        repository.insert(eventThree).futureValue

        val source = repository.find(searchRequest)
        val result = source.runWith(Sink.fold(Seq.empty[SearchResult])(_ :+ _)).futureValue

        result must contain only (
          SearchResult(
            eventOne.event.credId,
            eventOne.event.subjectId,
            eventOne.timeOfInterest,
            eventType        = eventOne.event.eventType,
            action           = eventOne.action,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventOne.event.eventData.asInstanceOf[AccountInterventionEventData].initiatingEntity,
            credentialType   = None,
            reasonAdmin      = None,
            reasonUser       = None,
            emailAddress     = None,
            interventionCode = None
          ),
          SearchResult(
            eventTwo.event.credId,
            eventTwo.event.subjectId,
            eventTwo.timeOfInterest,
            eventType        = eventTwo.event.eventType,
            action           = eventTwo.action,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventTwo.event.eventData.asInstanceOf[AccountInterventionEventData].initiatingEntity,
            credentialType   = None,
            reasonAdmin      = None,
            reasonUser       = None,
            emailAddress     = None,
            interventionCode = None
          ),
          SearchResult(
            eventThree.event.credId,
            eventThree.event.subjectId,
            eventThree.timeOfInterest,
            eventType        = eventThree.event.eventType,
            action           = eventThree.action,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventThree.event.eventData.asInstanceOf[AccountInterventionEventData].initiatingEntity,
            credentialType   = None,
            reasonAdmin      = None,
            reasonUser       = None,
            emailAddress     = None,
            interventionCode = None
          )
        )
      }

      "insert documents to collection and search AccountIntervention with dateFrom, dateTo, credId, subjectId, and eventType fields" in {
        given Materializer = app.materializer

        val subjectId1 = subjectIdGen.sample
        val subjectId2 = subjectIdGen.sample
        val subjectId3 = subjectIdGen.sample
        val credId1 = credIdGen.sample
        val credId2 = credIdGen.sample
        val credId3 = credIdGen.sample

        val searchRequest = validSearchRequestGen.sample.get.copy(
          credIds    = Some(Seq(credId1.get, credId2.get, credId3.get)),
          subjectIds = Some(Seq(subjectId1.get, subjectId2.get, subjectId3.get)),
          eventType  = Some(EventType.AccountIntervention)
        )
        val instantFrom = searchRequest.dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC)
        val instantTo = searchRequest.dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1)

        val eventOne = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = true).sample.get.copy(subjectId = subjectId1, credId = credId1))
        val eventTwo = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = false).sample.get.copy(subjectId = subjectId2, credId = credId2))
        val eventThree = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = false).sample.get.copy(subjectId = subjectId3, credId = credId3))

        repository.insert(eventOne).futureValue
        repository.insert(eventTwo).futureValue
        repository.insert(eventThree).futureValue

        val source = repository.find(searchRequest)
        val result = source.runWith(Sink.fold(Seq.empty[SearchResult])(_ :+ _)).futureValue

        result.length mustBe 1

        result must contain only (
          SearchResult(
            eventOne.event.credId,
            eventOne.event.subjectId,
            eventOne.timeOfInterest,
            eventType        = eventOne.event.eventType,
            action           = eventOne.action,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventOne.event.eventData.asInstanceOf[AccountInterventionEventData].initiatingEntity,
            credentialType   = None,
            reasonAdmin      = None,
            reasonUser       = None,
            emailAddress     = None,
            interventionCode = None
          )
        )
      }

      "the filters are credentialType, reasonAdmin, reasonUser, emailAddress, interventionCode and eventType AccountConcernDetails" in {
        given Materializer = app.materializer

        val searchRequest = validSearchRequestGen.sample.get.copy(
          credIds    = None,
          subjectIds = None,
          eventType  = Some(EventType.CredentialCompromise)
        )
        val instantFrom = searchRequest.dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC)
        val instantTo = searchRequest.dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1)

        val eventOne = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = false).sample.get)
        val eventTwo = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = false).sample.get)
        val eventThree = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = false).sample.get)
        val eventFour = eventToDocument(concernEventGen(instantFrom, instantTo).sample.get)
        val eventFive = eventToDocument(compromiseEventGen(instantFrom, instantTo).sample.get)

        repository.insert(eventOne).futureValue
        repository.insert(eventTwo).futureValue
        repository.insert(eventThree).futureValue
        repository.insert(eventFour).futureValue
        repository.insert(eventFive).futureValue

        val source = repository.find(searchRequest)
        val result = source.runWith(Sink.fold(Seq.empty[SearchResult])(_ :+ _)).futureValue

        result must contain only (
          SearchResult(
            eventFive.event.credId,
            eventFive.event.subjectId,
            eventFive.timeOfInterest,
            eventType        = eventFive.event.eventType,
            action           = None,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventFive.event.eventData.asInstanceOf[CredentialCompromiseEventData].initiatingEntity,
            credentialType   = eventFive.event.eventData.asInstanceOf[CredentialCompromiseEventData].credentialType,
            reasonAdmin      = eventFive.event.eventData.asInstanceOf[CredentialCompromiseEventData].reasonAdmin,
            reasonUser       = eventFive.event.eventData.asInstanceOf[CredentialCompromiseEventData].reasonUser,
            emailAddress     = eventFive.event.eventData.asInstanceOf[CredentialCompromiseEventData].emailAddress,
            interventionCode = eventFive.event.eventData.asInstanceOf[CredentialCompromiseEventData].interventionCode
          )
        )
      }

      "the filters are multiple credentialType, reasonAdmin, reasonUser, emailAddress, interventionCode and eventType AccountConcernDetails" in {
        given Materializer = app.materializer

        val searchRequest = validSearchRequestGen.sample.get.copy(
          credIds    = None,
          subjectIds = None,
          eventType  = Some(EventType.CredentialCompromise)
        )
        val instantFrom = searchRequest.dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC)
        val instantTo = searchRequest.dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1)

        val eventOne = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = false).sample.get)
        val eventTwo = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = false).sample.get)
        val eventThree = eventToDocument(interventionEventGen(instantFrom, instantTo, isAnalyst = false).sample.get)
        val eventFour = eventToDocument(concernEventGen(instantFrom, instantTo).sample.get)
        val eventFive = eventToDocument(compromiseEventGen(instantFrom, instantTo).sample.get)
        val eventSix = eventToDocument(compromiseEventGen(instantFrom, instantTo).sample.get)
        val eventSeven = eventToDocument(compromiseEventGen(instantFrom, instantTo).sample.get)

        repository.insert(eventOne).futureValue
        repository.insert(eventTwo).futureValue
        repository.insert(eventThree).futureValue
        repository.insert(eventFour).futureValue
        repository.insert(eventFive).futureValue
        repository.insert(eventSix).futureValue
        repository.insert(eventSeven).futureValue

        val source = repository.find(searchRequest)
        val result = source.runWith(Sink.fold(Seq.empty[SearchResult])(_ :+ _)).futureValue

        result must contain only (
          SearchResult(
            eventFive.event.credId,
            eventFive.event.subjectId,
            eventFive.timeOfInterest,
            eventType        = eventFive.event.eventType,
            action           = None,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventFive.event.eventData.asInstanceOf[CredentialCompromiseEventData].initiatingEntity,
            credentialType   = eventFive.event.eventData.asInstanceOf[CredentialCompromiseEventData].credentialType,
            reasonAdmin      = eventFive.event.eventData.asInstanceOf[CredentialCompromiseEventData].reasonAdmin,
            reasonUser       = eventFive.event.eventData.asInstanceOf[CredentialCompromiseEventData].reasonUser,
            emailAddress     = eventFive.event.eventData.asInstanceOf[CredentialCompromiseEventData].emailAddress,
            interventionCode = eventFive.event.eventData.asInstanceOf[CredentialCompromiseEventData].interventionCode
          ),
          SearchResult(
            eventSix.event.credId,
            eventSix.event.subjectId,
            eventSix.timeOfInterest,
            eventType        = eventSix.event.eventType,
            action           = None,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventSix.event.eventData.asInstanceOf[CredentialCompromiseEventData].initiatingEntity,
            credentialType   = eventSix.event.eventData.asInstanceOf[CredentialCompromiseEventData].credentialType,
            reasonAdmin      = eventSix.event.eventData.asInstanceOf[CredentialCompromiseEventData].reasonAdmin,
            reasonUser       = eventSix.event.eventData.asInstanceOf[CredentialCompromiseEventData].reasonUser,
            emailAddress     = eventSix.event.eventData.asInstanceOf[CredentialCompromiseEventData].emailAddress,
            interventionCode = eventSix.event.eventData.asInstanceOf[CredentialCompromiseEventData].interventionCode
          ),
          SearchResult(
            eventSeven.event.credId,
            eventSeven.event.subjectId,
            eventSeven.timeOfInterest,
            eventType        = eventSeven.event.eventType,
            action           = None,
            reason           = None,
            rationale        = None,
            initiatingEntity = eventSeven.event.eventData.asInstanceOf[CredentialCompromiseEventData].initiatingEntity,
            credentialType   = eventSeven.event.eventData.asInstanceOf[CredentialCompromiseEventData].credentialType,
            reasonAdmin      = eventSeven.event.eventData.asInstanceOf[CredentialCompromiseEventData].reasonAdmin,
            reasonUser       = eventSeven.event.eventData.asInstanceOf[CredentialCompromiseEventData].reasonUser,
            emailAddress     = eventSeven.event.eventData.asInstanceOf[CredentialCompromiseEventData].emailAddress,
            interventionCode = eventSeven.event.eventData.asInstanceOf[CredentialCompromiseEventData].interventionCode
          )
        )
      }
    }
  }
}
