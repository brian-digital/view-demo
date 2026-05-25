/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.test

import org.apache.pekko.actor.ActorSystem
import org.mongodb.scala.ObservableImplicits
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.Logging
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.blockandsignalsgdsstore.BaseISpec
import uk.gov.hmrc.blockandsignalsgdsstore.config.AppConfig
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.EventDocument
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.SearchRequest
import uk.gov.hmrc.blockandsignalsgdsstore.repositories.{EventDocumentRepository, GDSSearchView}

import java.time.{Clock, Duration, LocalDate}

class TestEventGeneratorServiceISpec extends BaseISpec with Logging with FutureAwaits with DefaultAwaitTimeout with ObservableImplicits {

  given defaultPatienceConfig: PatienceConfig =
    PatienceConfig(
      timeout  = scaled(Span(40, Seconds)),
      interval = scaled(Span(150, Millis))
    )

  given actorSystem: ActorSystem = ActorSystem("TestEventGeneratorServiceISpec")

  val appConfig: AppConfig = inject[AppConfig]

  val eventDocumentRepository: EventDocumentRepository = inject[EventDocumentRepository]

  val clock: Clock = inject[Clock]

  val gdsSearchView: GDSSearchView = inject[GDSSearchView]

  val testEventGeneratorService =
    new TestEventGeneratorService(appConfig, eventDocumentRepository, clock, gdsSearchView)

  "TestEventGeneratorService" should {
    "Generate Test events and save them to Mongo when randomly generate credIds" in {
      val eventCount = 10_000

      testEventGeneratorService.generateTestEvents(eventCount, randomlyGenerate = true).futureValue
      val start = clock.instant()
      val count = eventDocumentRepository
        .count(
          SearchRequest(
            LocalDate.now(clock).atStartOfDay(clock.getZone).minusMonths(18).toLocalDate,
            LocalDate.now(clock).plusDays(1).atStartOfDay(clock.getZone).minusNanos(1).toLocalDate,
            None,
            None,
            None
          )
        )
        .futureValue
      val end = clock.instant()
      logger.info(s"[GG-8340] count query duration: ${Duration.between(start, end).toString}")

      count shouldBe eventCount
    }

    "Generate Test events and save them to Mongo when not randomly generate credIds" in {
      val eventCount: Int = 1_000

      testEventGeneratorService.generateTestEvents(eventCount, randomlyGenerate = false).futureValue

      val records: Seq[EventDocument] = await(eventDocumentRepository.collection.find().toFuture())

      records.size shouldBe eventCount

      val credIds: Seq[Option[String]] = records.map(_.event.credId).distinct

      credIds.size shouldBe eventCount / 10

      val sortedIds = credIds.flatten.sorted
      sortedIds.head shouldBe "0000000000000001"
      sortedIds(1)   shouldBe "0000000000000002"
      sortedIds(50)  shouldBe "0000000000000051"
      sortedIds.last shouldBe "0000000000000100"
    }

  }

  override def beforeEach(): Unit = {
    eventDocumentRepository.deleteEvents().futureValue
  }

  override def afterEach(): Unit = {
    eventDocumentRepository.deleteEvents().futureValue
  }
}
