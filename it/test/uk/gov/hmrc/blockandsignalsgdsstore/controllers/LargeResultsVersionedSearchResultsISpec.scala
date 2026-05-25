/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.controllers

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Framing, Sink}
import org.apache.pekko.util.ByteString
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Logging}
import play.api.libs.json.{Format, Json, Reads, Writes}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.test.Helpers.USER_AGENT
import uk.gov.hmrc.blockandsignalsgdsstore.BaseISpec
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.EventType
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.{SearchRequest, SearchResult}
import uk.gov.hmrc.blockandsignalsgdsstore.repositories.{EventDocumentRepository, GDSSearchView}
import uk.gov.hmrc.blockandsignalsgdsstore.test.{Generators, TestEventRequest}

import java.time.{Clock, Instant, LocalDate}

class LargeResultsVersionedSearchResultsISpec extends BaseISpec with ScalaFutures with IntegrationPatience with Generators with ScalaCheckDrivenPropertyChecks with Logging {
  implicit val defaultPatienceConfig: PatienceConfig =
    PatienceConfig(
      timeout  = scaled(Span(40, Seconds)),
      interval = scaled(Span(150, Millis))
    )

  implicit val actorSystem: ActorSystem = ActorSystem("LargeResultsVersionedSearchResultsISpec")
  implicit val searchResultWrites: Writes[SearchResult] = SearchResult.httpWrites
  implicit val eventTypeReads: Reads[EventType] = EventType.searchValueReads
  implicit val httpReads: Reads[SearchResult] = Json.reads[SearchResult]
  implicit val testEventFormat: Format[TestEventRequest] = TestEventRequest.formats(clock)
  implicit val searchRequestFormat: Format[SearchRequest] = SearchRequest.searchRequestFormat(eventStoreConfig)

  val eventDocumentRepository: EventDocumentRepository = inject[EventDocumentRepository]
  val gdsSearchView: GDSSearchView = inject[GDSSearchView]
  val clock: Clock = inject[Clock]
  private val defaultFrom = LocalDate.now(clock).atStartOfDay(clock.getZone).minusMonths(18)
  private val defaultTo = LocalDate.now(clock).plusDays(1).atStartOfDay(clock.getZone).minusNanos(1)

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .configure(
        Map(
          "play.http.router"                     -> "testOnlyDoNotUseInAppConf.Routes",
          "search.result.limit"                  -> 100000,
          "test-only.event-generator.batch-size" -> 200,
          "feature.v1-search.use-view"           -> true
        )
      )
      .build()
  }

  override def beforeEach(): Unit = {
    eventDocumentRepository.deleteEvents().futureValue
    gdsSearchView.deleteEvents().futureValue
  }

  "POST /v2/search/results" should {
    "return a large result ok" in {
      val eventCount = 1_000

      val testEventRequest = TestEventRequest(eventCount)
      val insertStart = clock.instant()

      val createResponse = wsClient
        .url(s"$hostURL/test-only/event")
        .post(Json.toJson(testEventRequest))
        .futureValue

      createResponse.status shouldBe Status.CREATED
      val insertEnd = clock.instant()
      logger.info(s"[GG-8340] insert query duration: ${java.time.Duration.between(insertStart, insertEnd).toString}")

      val searchRequest = SearchRequest(
        defaultFrom.toLocalDate,
        defaultTo.toLocalDate,
        None,
        None,
        None
      )

      eventDocumentRepository.count(searchRequest).futureValue shouldBe eventCount

      val searchStart = clock.instant()

      val response = wsClient
        .url(s"$baseUrl/v2/search/results")
        .withMethod("POST")
        .withHttpHeaders(USER_AGENT -> "block-and-signals-frontend")
        .withBody(Json.toJson(searchRequest))
        .stream()
        .futureValue

      response.status shouldBe 200
      val chunkedResponseCount = response.bodyAsSource
        .via(Framing.delimiter(ByteString("\n"), Int.MaxValue))
        .map(_ => 1)
        .runWith(Sink.fold[Int, Int](0)(_ + _))
        .futureValue

      val searchEnd = clock.instant()

      chunkedResponseCount.longValue shouldBe eventCount

      logger.info(s"[GG-8340] search query duration: ${java.time.Duration.between(searchStart, searchEnd).toString}")

    }
  }
}
