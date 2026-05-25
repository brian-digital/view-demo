/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.controllers

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{Format, JsNull, Json, Reads, Writes}
import play.api.http.HeaderNames.USER_AGENT
import play.api.http.Status.FORBIDDEN
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.blockandsignalsgdsstore.BaseISpec
import uk.gov.hmrc.blockandsignalsgdsstore.adapters.SignalProcessorEventAdapters
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.EventType
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.{Event, EventDocument}
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.{SearchRequest, SearchResult, SearchResultV2}
import uk.gov.hmrc.blockandsignalsgdsstore.repositories.{EventDocumentRepository, GDSSearchView}
import uk.gov.hmrc.blockandsignalsgdsstore.test.Generators

import java.time.{Clock, LocalDate, ZoneId}

class VersionedSearchControllerISpec extends BaseISpec with ScalaFutures with IntegrationPatience with Generators with ScalaCheckDrivenPropertyChecks {
  implicit val defaultPatienceConfig: PatienceConfig =
    PatienceConfig(
      timeout  = scaled(Span(40, Seconds)),
      interval = scaled(Span(150, Millis))
    )

  implicit val searchResultWrites: Writes[SearchResult] = SearchResult.httpWrites
  implicit val eventTypeReads: Reads[EventType] = EventType.searchValueReads
  implicit val httpReads: Reads[SearchResult] = Json.reads[SearchResult]
  implicit val searchRequestFormat: Format[SearchRequest] = SearchRequest.searchRequestFormat(eventStoreConfig)

  val eventDocumentRepository: EventDocumentRepository = inject[EventDocumentRepository]
  val gdsSearchView: GDSSearchView = inject[GDSSearchView]

  implicit val clock: Clock = Clock.systemUTC()

  private val defaultFrom = LocalDate.now(clock).atStartOfDay(clock.getZone).minusMonths(18)
  private val defaultTo = LocalDate.now(clock).plusDays(1).atStartOfDay(clock.getZone).minusNanos(1)

  "POST v2/search/results" should {

    "return search results for accountIntervention: for accountIntervention search only InitiatingEntity-analyst will return" in {

      val eventIsAnalystInitiatingEntity: Event = interventionEventGen(isAnalyst = true).sample.get
      val eventIsNotAnalystInitiatingEntity: Event = interventionEventGen(isAnalyst = false).sample.get

      val isAnalystDocument =
        SignalProcessorEventAdapters.eventToEventDocument(eventIsAnalystInitiatingEntity).toOption.get

      val isNotAnalystDocument =
        SignalProcessorEventAdapters.eventToEventDocument(eventIsNotAnalystInitiatingEntity).toOption.get

      gdsSearchView.deleteEvents().futureValue
      eventDocumentRepository.deleteEvents().futureValue
      eventDocumentRepository.insertMany(Seq(isAnalystDocument, isNotAnalystDocument)).futureValue
      gdsSearchView.updateMaterializedViewForAllRecords().futureValue

      val searchRequest = SearchRequest(
        dateFrom   = isAnalystDocument.timeOfInterest.atZone(ZoneId.of("UTC")).minusMinutes(1).toLocalDate,
        dateTo     = isAnalystDocument.timeOfInterest.atZone(ZoneId.of("UTC")).plusMinutes(1).toLocalDate,
        credIds    = None,
        subjectIds = None,
        eventType  = Some(EventType.AccountIntervention)
      )

      val response = wsClient
        .url(s"$baseUrl/v2/search/results")
        .withHttpHeaders(USER_AGENT -> "block-and-signals-frontend")
        .post(Json.toJson(searchRequest))
        .futureValue

      response.status shouldBe 200

      given Format[SearchResultV2] = SearchResultV2.httpFormat
      val responseBody = response.body.split("\n").map(Json.parse).map(_.as[SearchResultV2])
      responseBody.length shouldBe 1
    }

    "return search results for the the other events" in {
      forAll(eventsConcernEventBetweenGen(defaultFrom.toInstant, defaultTo.toInstant)) { events =>

        val documents = events.map { event =>
          SignalProcessorEventAdapters.eventToEventDocument(event).toOption.get
        }

        gdsSearchView.deleteEvents().futureValue
        eventDocumentRepository.deleteEvents().futureValue
        eventDocumentRepository.insertMany(documents).futureValue
        gdsSearchView.updateMaterializedViewForAllRecords().futureValue

        val searchRequest = SearchRequest(
          dateFrom   = defaultFrom.toLocalDate,
          dateTo     = defaultTo.toLocalDate,
          credIds    = None,
          subjectIds = None,
          eventType  = None
        )

        val response = wsClient
          .url(s"$baseUrl/v2/search/results")
          .withHttpHeaders(USER_AGENT -> "block-and-signals-frontend")
          .post(Json.toJson(searchRequest))
          .futureValue

        response.status shouldBe 200
        given Format[SearchResultV2] = SearchResultV2.httpFormat
        val responseBody = response.body.split("\n").map(Json.parse).map(_.as[SearchResultV2])
        responseBody.length shouldBe events.size
      }
    }

    "return 403 when there is no user agent" in {
      val response = wsClient
        .url(s"$baseUrl/v2/search/results")
        .post(JsNull)
        .futureValue
      response.status shouldBe FORBIDDEN
    }

    "return 403 when the user agent is not allowed" in {
      val response = wsClient
        .url(s"$baseUrl/search/results")
        .withHttpHeaders(USER_AGENT -> "invalid user agent")
        .post(JsNull)
        .futureValue
      response.status shouldBe FORBIDDEN
    }
  }

  "POST /search/count" should {

    "return count correctly: for accountIntervention only InitiatingEntity-analyst will count" in {
      val eventIsAnalystInitiatingEntity: Event = interventionEventGen(isAnalyst = true).sample.get
      val eventIsNotAnalystInitiatingEntity: Event = interventionEventGen(isAnalyst = false).sample.get

      val isAnalystDocument =
        SignalProcessorEventAdapters.eventToEventDocument(eventIsAnalystInitiatingEntity).toOption.get

      val isNotAnalystDocument =
        SignalProcessorEventAdapters.eventToEventDocument(eventIsNotAnalystInitiatingEntity).toOption.get

      gdsSearchView.deleteEvents().futureValue
      eventDocumentRepository.deleteEvents().futureValue
      eventDocumentRepository.insertMany(Seq(isAnalystDocument, isNotAnalystDocument)).futureValue
      gdsSearchView.updateMaterializedViewForAllRecords().futureValue

      val searchRequest = SearchRequest(
        dateFrom   = isAnalystDocument.timeOfInterest.atZone(ZoneId.of("UTC")).minusMinutes(1).toLocalDate,
        dateTo     = isAnalystDocument.timeOfInterest.atZone(ZoneId.of("UTC")).plusMinutes(1).toLocalDate,
        credIds    = None,
        subjectIds = None,
        eventType  = Some(EventType.AccountIntervention)
      )

      val response = wsClient
        .url(s"$baseUrl/v2/search/count")
        .withHttpHeaders(USER_AGENT -> "block-and-signals-frontend")
        .post(Json.toJson(searchRequest))
        .futureValue

      response.status shouldBe 200
      response.json   shouldBe Json.obj("count" -> 1)
    }

    "return count correctly" in {
      forAll(eventsConcernEventBetweenGen(defaultFrom.toInstant, defaultTo.toInstant)) { events =>

        val documents = events.map { event =>
          SignalProcessorEventAdapters.eventToEventDocument(event).toOption.get
        }

        gdsSearchView.deleteEvents().futureValue
        eventDocumentRepository.deleteEvents().futureValue
        eventDocumentRepository.insertMany(documents).futureValue
        gdsSearchView.updateMaterializedViewForAllRecords().futureValue

        val searchRequest = SearchRequest(
          dateFrom   = defaultFrom.toLocalDate,
          dateTo     = defaultTo.toLocalDate,
          credIds    = None,
          subjectIds = None,
          eventType  = None
        )
        val response = wsClient
          .url(s"$baseUrl/v2/search/count")
          .withHttpHeaders(USER_AGENT -> "block-and-signals-frontend")
          .post(Json.toJson(searchRequest))
          .futureValue

        response.status shouldBe 200
        response.json   shouldBe Json.obj("count" -> events.size)
      }
    }

    "return 403 when there is no user agent" in {
      val response = wsClient
        .url(s"$baseUrl/v2/search/count")
        .post(JsNull)
        .futureValue
      response.status shouldBe FORBIDDEN
    }

    "return 403 when the user agent is not allowed" in {
      val response = wsClient
        .url(s"$baseUrl/v2/search/count")
        .withHttpHeaders(USER_AGENT -> "invalid user agent")
        .post(JsNull)
        .futureValue
      response.status shouldBe FORBIDDEN
    }
  }
}
