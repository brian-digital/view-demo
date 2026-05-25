/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.filters

import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import play.api.http.Status.{CREATED, OK}
import play.api.http.{ContentEncoding, HeaderNames}
import play.api.libs.json.{Format, JsString, Json, Writes}
import play.api.test.FakeRequest
import play.api.test.Helpers.{ACCEPT_ENCODING, CONTENT_ENCODING, await, contentAsBytes, contentAsString, defaultAwaitTimeout, header, route, status, writeableOf_AnyContentAsJson}
import uk.gov.hmrc.blockandsignalsgdsstore.BaseISpec
import uk.gov.hmrc.blockandsignalsgdsstore.controllers.routes
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.SearchRequest
import uk.gov.hmrc.blockandsignalsgdsstore.repositories.EventDocumentRepository
import uk.gov.hmrc.blockandsignalsgdsstore.utils.TestData

import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.zip.GZIPInputStream
import scala.io.Source
import scala.util.Using

class GzipFilterIntegrationISpec extends BaseISpec with OptionValues with TestData {

  given Materializer = app.materializer

  implicit val searchRequestFormat: Format[SearchRequest] = SearchRequest.searchRequestFormat(eventStoreConfig)

  val eventDocumentRepository: EventDocumentRepository = app.injector.instanceOf[EventDocumentRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(eventDocumentRepository.collection.deleteMany(Filters.exists("_id")).toFuture())
  }

  override def afterEach(): Unit = {
    super.afterEach()
    await(eventDocumentRepository.collection.deleteMany(Filters.exists("_id")).toFuture())
  }

  private def decompressGzip(compressedBytes: ByteString): String = {
    val gzipStream = Using(new GZIPInputStream(new ByteArrayInputStream(compressedBytes.toArray))) { gzipStream =>
      Source.fromInputStream(gzipStream).mkString
    }
    gzipStream.get
  }

  "POST /search/results" should {

    s"return gzipped response of the inserted events if '$ACCEPT_ENCODING' header was specified" in {
      val insertRequest = FakeRequest("POST", routes.EventController.storeEvent("v1").url)
        .withHeaders(HeaderNames.USER_AGENT -> "block-and-signals-frontend")
        .withJsonBody(Json.toJson(EventRequestV1Json.accountConcernJsonFull))

      val insertResult = route(app, insertRequest).value
      status(insertResult) shouldBe CREATED

      val searchRequestBody = SearchRequest(LocalDate.of(2017, 10, 10), LocalDate.now, None, None, None)

      val searchRequest = FakeRequest("POST", routes.SearchController.search().url)
        .withHeaders(
          ACCEPT_ENCODING        -> ContentEncoding.Gzip,
          HeaderNames.USER_AGENT -> "block-and-signals-frontend"
        )
        .withJsonBody(Json.toJson(searchRequestBody))
      val searchResult = route(app, searchRequest).value

      status(searchResult)                         shouldBe OK
      header(CONTENT_ENCODING, searchResult).value shouldBe ContentEncoding.Gzip

      val jsonLines = decompressGzip(contentAsBytes(searchResult)).split("\n").toSeq.map(Json.parse)

      jsonLines.size shouldBe 1

      (jsonLines.head \ "credId").get shouldBe JsString("3434343434343434")
      (jsonLines.head \ "subjectId").get shouldBe JsString(
        "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw="
      )
      (jsonLines.head \ "timeOfInterest").get shouldBe JsString("2017-10-10T14:16:37.001Z")
      (jsonLines.head \ "eventType").get      shouldBe JsString("account-concern")
      (jsonLines.head \ "reason").get         shouldBe JsString("account-takeover")
      (jsonLines.head \ "rationale").get      shouldBe JsString("RA99")
    }

    s"return non-gzipped response of the inserted events if '$ACCEPT_ENCODING' header was not specified" in {
      val insertRequest = FakeRequest("POST", routes.EventController.storeEvent("v1").url)
        .withHeaders(HeaderNames.USER_AGENT -> "block-and-signals-frontend")
        .withJsonBody(Json.toJson(EventRequestV1Json.accountConcernJsonFull))

      val insertResult = route(app, insertRequest).value
      status(insertResult) shouldBe CREATED

      val searchRequestBody = SearchRequest(LocalDate.of(2017, 10, 10), LocalDate.now, None, None, None)

      val searchRequest = FakeRequest("POST", routes.SearchController.search().url)
        .withHeaders(HeaderNames.USER_AGENT -> "block-and-signals-frontend")
        .withJsonBody(Json.toJson(searchRequestBody))
      val searchResult = route(app, searchRequest).value

      status(searchResult)                   shouldBe OK
      header(CONTENT_ENCODING, searchResult) shouldBe empty

      val jsonLines = contentAsString(searchResult).split("\n").toSeq.map(Json.parse)

      jsonLines.size shouldBe 1

      (jsonLines.head \ "credId").get shouldBe JsString("3434343434343434")
      (jsonLines.head \ "subjectId").get shouldBe JsString(
        "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw="
      )
      (jsonLines.head \ "timeOfInterest").get shouldBe JsString("2017-10-10T14:16:37.001Z")
      (jsonLines.head \ "eventType").get      shouldBe JsString("account-concern")
      (jsonLines.head \ "reason").get         shouldBe JsString("account-takeover")
      (jsonLines.head \ "rationale").get      shouldBe JsString("RA99")
    }
  }
}
