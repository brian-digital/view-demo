/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.repositories

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.{Done, NotUsed}
import org.bson.codecs.Codec
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.mongodb.scala.bson.BsonTransformer
import org.mongodb.scala.model.*
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.result.DeleteResult
import org.mongodb.scala.{Document, MongoException, Observer, WriteConcern}
import uk.gov.hmrc.blockandsignalsgdsstore.config.{AppConfig, EventStoreConfig}
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.DeviceConcernIdentifierType
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.EventType
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.{SearchRequest, SearchResultV2}
import uk.gov.hmrc.blockandsignalsgdsstore.models.{common, db}
import uk.gov.hmrc.blockandsignalsgdsstore.services.SearchFilterV2
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.time.{Clock, Duration, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class GDSSearchView @Inject() (
  mongoComponent: MongoComponent,
  eventStoreConfig: EventStoreConfig,
  appConfig: AppConfig,
  searchFilter: SearchFilterV2
)(implicit ec: ExecutionContext, clock: Clock)
    extends MongoMaterializedView[SearchResultV2](
      mongoComponent       = mongoComponent,
      viewName             = "gdsSearch",
      sourceCollectionName = "events",
      domainFormat         = SearchResultV2.mongoFormat,
      extraCodecs          = Codecs.playFormatSumCodecs(EventType.mongoFormat),
      indexes = Seq(
        IndexModel(
          ascending("storedAt"),
          IndexOptions()
            .name("storedAtIndex")
            .expireAfter(eventStoreConfig.eventTtl.length, eventStoreConfig.eventTtl.unit)
        ),
        IndexModel(
          ascending("submittedOn"),
          indexOptions = IndexOptions().name("submittedOnIndex")
        ),
        IndexModel(
          Indexes.compoundIndex(
            Indexes.ascending("eventType"),
            Indexes.ascending("submittedBy")
          ),
          IndexOptions().name("eventTypeAndSubmittedByIndex")
        ),
        IndexModel(
          Indexes.compoundIndex(
            Indexes.ascending("eventType"),
            Indexes.ascending("submittedOn")
          ),
          IndexOptions().name("eventTypeSubmittedOnIndex")
        ),
        IndexModel(
          Indexes.compoundIndex(
            Indexes.ascending("oneLoginSubjectId"),
            Indexes.ascending("submittedOn")
          ),
          IndexOptions().name("oneLoginSubjectIdAndSubmittedOnIndex")
        ),
        IndexModel(
          Indexes.compoundIndex(
            Indexes.ascending("hmrcCredentialId"),
            Indexes.ascending("submittedOn")
          ),
          IndexOptions().name("hmrcCredentialIdAndSubmittedOnIndex")
        )
      )
    ) {

  import GDSSearchView.*

  override protected lazy val aggregationStages: Seq[Bson] = ProjectionPipeline.pipeline
  override protected lazy val initializeViewOnStartup: Boolean = appConfig.featureToggles.v1SearchUseViewEnabled

  def updateMaterializedViewForSingleRecord(objectId: ObjectId): Future[Either[String, Unit]] =
    val idFilter = Filters.equal("_id", objectId)
    updateMaterializedView(idFilter)
      .map(_ => Right(()))
      .recover { case err: MongoException =>
        val details = s"Event Mongo ID: ${objectId.toString}, Mongo error code: ${err.getCode}"
        val message = s"Failed to update search view for single record. $details"
        logger.error(message)
        Left(message)
      }

  def updateMaterializedViewForManyRecords(objectIds: List[ObjectId]): Future[Either[String, Unit]] =
    val idFilter = Filters.in("_id", objectIds*)
    updateMaterializedView(idFilter)
      .map(_ => Right(()))
      .recover { case err: MongoException =>
        val details = s"Record count: ${objectIds.length}, Mongo error code: ${err.getCode}"
        val message = s"Failed to update search view for many records. $details"
        logger.error(message)
        Left(message)
      }

  def updateMaterializedViewForAllRecords(): Future[Either[String, Unit]] =
    updateMaterializedView()
      .map(_ => Right(()))
      .recover { case err: MongoException =>
        val details = s"Mongo error code: ${err.getCode}"
        val message = s"Failed to update search view for all records. $details"
        logger.error(message)
        Left(message)
      }

  def deleteEvents(filters: Bson = Filters.empty): Future[DeleteResult] = {
    materializedView.deleteMany(filters).toFuture()
  }

  def clearViewUsingTTL(): Future[Done] = {
    val updates: Bson = Updates.combine(
      Updates.set("storedAt", Instant.now(clock).minus(Duration.ofNanos(eventStoreConfig.eventTtl.toNanos)))
    )
    materializedView
      .withWriteConcern(WriteConcern.UNACKNOWLEDGED)
      .updateMany(Filters.empty(), updates)
      .toFuture()
      .map(_ => Done)
  }

  def dropAndRecreate: Future[Try[Unit]] =
    materializedView
      .drop()
      .toFuture()
      .map(_ => initializeView)

  def find(searchRequest: SearchRequest): Source[SearchResultV2, NotUsed] = {
    val searchResultsSource =
      materializedView
        .find[SearchResultV2](searchFilter.buildFilter(searchRequest))
        .batchSize(eventStoreConfig.mongoSearchResultBatchSize)
        .limit(eventStoreConfig.searchResultLimit)

    val queryStart = clock.instant()
    logger.info(s"[VER-6642] GDSSearch view.find() start: $queryStart")

    searchResultsSource
      .subscribe(new Observer[SearchResultV2] {
        override def onNext(result: SearchResultV2): Unit = ()

        override def onError(e: Throwable): Unit = ()

        override def onComplete(): Unit = {
          val queryEnd = clock.instant()
          logger.info(s"[VER-6642] GDSSearch view.find() end: $queryEnd")
          logger.info(s"[VER-6642] GDSSearch view.find() duration: ${Duration.between(queryStart, queryEnd).toString}")
        }
      })

    Source.fromPublisher(searchResultsSource)
  }

  def findFirst(): Future[Option[SearchResultV2]] =
    materializedView.find[SearchResultV2]().limit(1).headOption()

  def count(searchRequest: SearchRequest): Future[Long] =
    materializedView.countDocuments(searchFilter.buildFilter(searchRequest)).toFuture()

}

object GDSSearchView {

  private def bsonOr(firstChoiceExpression: String, secondChoiceExpression: String): Document =
    Document(
      "$cond" -> Document(
        "if"   -> firstChoiceExpression,
        "then" -> firstChoiceExpression,
        "else" -> secondChoiceExpression
      )
    )

  private object EventDataProjections {

    val accountConcern: Document =
      Document(
        "submittedBy" -> "$event.eventData.initiatingEntity",
        "reason"      -> "$event.eventData.reason",
        "rationale"   -> "$event.eventData.rationale"
      )

    val accountIntervention: Document =
      Document(
        "submittedBy"               -> "$event.eventData.initiatingEntity",
        "reason"                    -> "$event.eventData.reason",
        "rationale"                 -> "$event.eventData.rationale",
        "accountInterventionState"  -> "$event.eventData.state",
        "accountInterventionAction" -> "$event.eventData.action"
      )

    val credentialCompromise: Document =
      Document(
        "submittedBy"                          -> "$event.eventData.initiatingEntity",
        "reason"                               -> bsonOr("$event.eventData.reasonAdmin", "$event.eventData.reasonUser"),
        "rationale"                            -> "$event.eventData.rationale",
        "credentialCompromiseEmailAddress"     -> "$event.eventData.emailAddress",
        "credentialCompromiseInterventionCode" -> "$event.eventData.interventionCode"
      )

    val credentialConcern: Document =
      Document(
        "submittedBy"                       -> "$event.eventData.initiatingEntity",
        "reason"                            -> "$event.eventData.reasonAdmin",
        "rationale"                         -> "$event.eventData.rationale",
        "credentialConcernSourceType"       -> "$event.eventData.sourceType",
        "credentialConcernSourceUri"        -> "$event.eventData.sourceTypeUri",
        "credentialConcernCredentialType"   -> "$event.eventData.credentialType",
        "credentialConcernIdentifierFormat" -> "$event.eventData.identifierFormat",
        "credentialConcernDocumentNumber"   -> "$event.eventData.documentNumber",
        "credentialConcernExpiryDate"       -> "$event.eventData.expiryDate",
        "credentialConcernIcaoIssuerCode"   -> "$event.eventData.icaoIssuerCode",
        "credentialConcernPersonalNumber"   -> "$event.eventData.personalNumber",
        "credentialConcernIssueNumber"      -> "$event.eventData.issueNumber",
        "credentialConcernIssuedBy"         -> "$event.eventData.issuedBy",
        "credentialConcernIssuingCountry"   -> "$event.eventData.issuingCountry",
        "credentialConcernEmailAddress"     -> "$event.eventData.emailAddress",
        "credentialConcernTelephoneNumber"  -> "$event.eventData.phoneNumber",
        "credentialConcernNino"             -> "$event.eventData.nino"
      )

    val deviceConcern: Document =
      Document(
        "$mergeObjects" -> Seq(
          DeviceIdentifierProjection.extractDeviceIdentifiers,
          Document(
            "submittedBy"             -> "$event.eventData.initiatingEntity",
            "reason"                  -> "$event.eventData.reasonAdmin",
            "rationale"               -> "$event.eventData.rationale",
            "deviceConcernSourceType" -> "$event.eventData.sourceType",
            "deviceConcernSourceUri"  -> "$event.eventData.sourceTypeUri"
          )
        )
      )
  }

  private object DeviceIdentifierProjection {

    private def deviceTypeFieldName(deviceType: DeviceConcernIdentifierType): String =
      deviceType match {
        case DeviceConcernIdentifierType.DeviceHash          => "deviceConcernDeviceHash"
        case DeviceConcernIdentifierType.DeviceId            => "deviceConcernDeviceId"
        case DeviceConcernIdentifierType.JourneyId           => "deviceConcernJourneyId"
        case DeviceConcernIdentifierType.PersistentSessionId => "deviceConcernPersistentSessionCookie"
        case DeviceConcernIdentifierType.SessionId           => "deviceConcernSessionId"
        case DeviceConcernIdentifierType.UserDeviceIpAddress => "deviceConcernIpAddress"
      }

    private val deviceTypeSwitch = {
      val branches: Seq[Document] = DeviceConcernIdentifierType.values.toSeq.map { deviceType =>
        Document(
          "case" -> Document("$eq" -> Seq("$$identifier.format", deviceType.stringValue)),
          "then" -> deviceTypeFieldName(deviceType)
        )
      }
      Document(
        "$switch" -> Document(
          "branches" -> branches
        )
      )
    }

    val extractDeviceIdentifiers: Document =
      Document(
        "$arrayToObject" -> Document(
          "$map" -> Document(
            "input" -> "$event.eventData.identifiers",
            "as"    -> "identifier",
            "in" -> Document(
              "k" -> deviceTypeSwitch,
              "v" -> "$$identifier.value"
            )
          )
        )
      )
  }

  object ProjectionPipeline {

    private val projectCommonFields: Bson = Projections.fields(
      Projections.computed("eventType", "$event.eventType"),
      Projections.computed("hmrcCredentialId", "$event.credId"),
      Projections.computed("oneLoginSubjectId", "$event.subjectId"),
      Projections.computed("submittedOn", "$timeOfInterest"),
      Projections.computed("storedAt", "$storedAt")
    )

    private val projectionEventDataSwitch: Bson = {
      val branches: Seq[Document] = EventType.values.toSeq.map { eventType =>
        val projectionDocument = eventType match
          case EventType.AccountConcern       => EventDataProjections.accountConcern
          case EventType.AccountIntervention  => EventDataProjections.accountIntervention
          case EventType.CredentialCompromise => EventDataProjections.credentialCompromise
          case EventType.CredentialConcern    => EventDataProjections.credentialConcern
          case EventType.DeviceConcern        => EventDataProjections.deviceConcern

        Document(
          "case" -> Document("$eq" -> Seq("$event.eventType", eventType.mongoValue)),
          "then" -> projectionDocument
        )
      }

      Projections.computed(
        fieldName = "eventData",
        expression = Document(
          "$switch" -> Document(
            "branches" -> branches
          )
        )
      )
    }

    private val projectEvents: Bson = Aggregates.project(
      Projections.fields(
        projectCommonFields,
        projectionEventDataSwitch
      )
    )

    private val mergeEventData: Bson =
      Aggregates.replaceRoot(Document("$mergeObjects" -> Seq("$eventData", "$$ROOT")))

    private val excludeOriginalEventData: Bson =
      Aggregates.project(
        Projections.fields(
          Projections.excludeId(),
          Projections.exclude("eventData")
        )
      )

    val pipeline: Seq[Bson] =
      Seq(projectEvents, mergeEventData, excludeOriginalEventData)
  }
}
