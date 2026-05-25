/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.test

import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.scalatest.OptionValues
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.HeaderNames.USER_AGENT
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Format, JsObject, Json}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.*
import play.api.{Application, Logging}
import uk.gov.hmrc.blockandsignalsgdsstore.BaseISpec
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.EventDocument
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.SearchRequest
import uk.gov.hmrc.blockandsignalsgdsstore.repositories.EventDocumentRepository
import uk.gov.hmrc.blockandsignalsgdsstore.utils.TestData

import java.time.{Clock, Duration, Instant, LocalDate}

class TestEventControllerISpec extends BaseISpec with OptionValues with IntegrationPatience with Generators with ScalaCheckDrivenPropertyChecks with Logging with TestData {

  implicit val defaultPatienceConfig: PatienceConfig =
    PatienceConfig(
      timeout  = scaled(Span(40, Seconds)),
      interval = scaled(Span(150, Millis))
    )

  val eventDocumentRepository: EventDocumentRepository = inject[EventDocumentRepository]
  val clock: Clock = inject[Clock]

  implicit val testEventFormat: Format[TestEventRequest] = TestEventRequest.formats(clock)

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .configure(
        Map(
          "play.http.router" -> "testOnlyDoNotUseInAppConf.Routes"
        )
      )
      .build()
  }

  "POST /test-only/event" should {

    "create test events when randomlyGenerate is true - default" in {
      val eventCount = 1000

      val testEventRequest: JsObject = {
        Json.obj(
          "eventCount" -> eventCount,
          "dateFrom"   -> clock.instant(),
          "dateTo"     -> clock.instant()
        )
      }

      val createResponse = wsClient.url(s"$hostURL/test-only/event").post(testEventRequest).futureValue
      createResponse.status shouldBe Status.CREATED

      val searchRequest = SearchRequest(
        LocalDate.now(clock).atStartOfDay(clock.getZone).toLocalDate,
        LocalDate.now(clock).plusDays(1).atStartOfDay(clock.getZone).minusNanos(1).toLocalDate,
        None,
        None,
        None
      )
      eventDocumentRepository.count(searchRequest).futureValue shouldBe eventCount
    }

    "create test events when randomlyGenerate is false" in {
      val eventCount: Int = 1000

      val testEventRequest: JsObject = {
        Json.obj(
          "eventCount"       -> eventCount,
          "dateFrom"         -> clock.instant(),
          "dateTo"           -> clock.instant(),
          "randomlyGenerate" -> false
        )
      }

      val createResponse: WSResponse = wsClient.url(s"$hostURL/test-only/event").post(testEventRequest).futureValue

      createResponse.status shouldBe Status.CREATED

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

  "DELETE /test-only/event" should {
    "delete all records when some exist" in {
      val createResponse: WSResponse =
        wsClient
          .url(s"$baseUrl/v1/event")
          .withHttpHeaders(USER_AGENT -> "block-and-signals-frontend")
          .post(EventRequestV1Json.accountConcernJsonFull)
          .futureValue
      createResponse.status shouldBe Status.CREATED

      await(eventDocumentRepository.collection.countDocuments().toFuture()) > 0 shouldBe true

      val deleteResponse = wsClient.url(s"$hostURL/test-only/event").delete().futureValue
      deleteResponse.status shouldBe Status.NO_CONTENT

      await(eventDocumentRepository.collection.countDocuments().toFuture()) shouldBe 0
    }
    "delete no records when none exist" in {
      val deleteResponse = wsClient.url(s"$hostURL/test-only/event").delete().futureValue
      deleteResponse.status                                                 shouldBe Status.NO_CONTENT
      await(eventDocumentRepository.collection.countDocuments().toFuture()) shouldBe 0
    }
  }

  "DELETE /test-only/clearDB" should {
    "set ttl on all records to near future" in {

      val priorToTestTime: Instant = Instant.now(clock)

      val createResponseOne: WSResponse =
        wsClient
          .url(s"$baseUrl/v1/event")
          .withHttpHeaders(USER_AGENT -> "block-and-signals-frontend")
          .post(EventRequestV1Json.accountConcernJsonFull)
          .futureValue
      createResponseOne.status shouldBe Status.CREATED

      val createResponseTwo: WSResponse =
        wsClient
          .url(s"$baseUrl/v1/event")
          .withHttpHeaders(USER_AGENT -> "block-and-signals-frontend")
          .post(EventRequestV1Json.accountInterventionJsonFullWithJti("123456789"))
          .futureValue
      createResponseTwo.status shouldBe Status.CREATED

      await(eventDocumentRepository.collection.countDocuments().toFuture()) shouldBe 2
      await(eventDocumentRepository.collection.find().toFuture()).foreach { doc =>
        doc.storedAt.toEpochMilli >= priorToTestTime.toEpochMilli                 shouldBe true
        doc.storedAt.toEpochMilli > priorToTestTime.minusSeconds(60).toEpochMilli shouldBe true
      }

      val deleteResponse = wsClient.url(s"$hostURL/test-only/clearDB").delete().futureValue
      deleteResponse.status shouldBe Status.NO_CONTENT

      await(eventDocumentRepository.collection.countDocuments().toFuture()) shouldBe 2
      await(eventDocumentRepository.collection.find().toFuture()).foreach { doc =>
        doc.storedAt.toEpochMilli < priorToTestTime.toEpochMilli shouldBe true
        doc.storedAt.toEpochMilli <= priorToTestTime
          .minus(Duration.ofNanos(eventStoreConfig.eventTtl.toNanos))
          .toEpochMilli shouldBe true
      }
    }

    "handle no records in db" in {
      val deleteResponse = wsClient.url(s"$hostURL/test-only/event").delete().futureValue
      deleteResponse.status                                                 shouldBe Status.NO_CONTENT
      await(eventDocumentRepository.collection.countDocuments().toFuture()) shouldBe 0
    }

  }

  override def beforeEach(): Unit = {
    eventDocumentRepository.deleteEvents().futureValue
  }

  override def afterEach(): Unit = {
    eventDocumentRepository.deleteEvents().futureValue
  }
}
