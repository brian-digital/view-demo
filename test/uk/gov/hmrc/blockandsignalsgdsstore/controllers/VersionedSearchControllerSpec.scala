/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.controllers

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{Format, Json, Reads, Writes}
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.blockandsignalsgdsstore.config.EventStoreConfig
import uk.gov.hmrc.blockandsignalsgdsstore.controllers.actions.UserAgentFilter
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.EventType
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.{SearchRequest, SearchResultV2}
import uk.gov.hmrc.blockandsignalsgdsstore.services.SearchServiceV2
import uk.gov.hmrc.blockandsignalsgdsstore.test.Generators
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class VersionedSearchControllerSpec extends AnyWordSpec with Matchers with Generators with ScalaCheckDrivenPropertyChecks with ScalaFutures {

  trait Setup {
    implicit val system: ActorSystem = ActorSystem("TestActorSystem")
    implicit val materializer: Materializer = SystemMaterializer(system).materializer
    implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val eventTypeReads: Reads[EventType] = EventType.searchValueReads

    val mockSearchServiceV2: SearchServiceV2 = mock[SearchServiceV2]
    val eventStoreConfig: EventStoreConfig = EventStoreConfig(
      searchResultLimit                 = 100,
      eventTtl                          = FiniteDuration(1, "day"),
      searchResultsChunkSize            = 500,
      searchResultsChunkWindow          = FiniteDuration(150, "millis"),
      searchRequestMaxAllowedIds        = 20,
      searchRequestMaxAllowedCredIds    = 20,
      searchRequestMaxAllowedSubjectIds = 20,
      mongoSearchResultBatchSize        = 1000
    )
    implicit val searchRequestFormat: Format[SearchRequest] = SearchRequest.searchRequestFormat(eventStoreConfig)

    private val mockUserAgentFilter: UserAgentFilter = new UserAgentFilter {
      override protected def filter[A](request: Request[A]): Future[Option[Result]] = Future.successful(None)

      override protected def executionContext: ExecutionContext = ec
    }
    val searchControllerV2: VersionedSearchController = new VersionedSearchController(
      Helpers.stubControllerComponents(),
      mockSearchServiceV2,
      eventStoreConfig,
      mockUserAgentFilter
    )

    def fakePostRequest[T](body: T): FakeRequest[T] = FakeRequest("POST", "/").withBody(body)

  }

  "SearchController /search/results" should {

    "return searchResults" in new Setup {
      forAll(validSearchRequestGen, searchResultsV2Gen) { (searchRequest, searchResultsV2) =>
        when(mockSearchServiceV2.count(eqTo(searchRequest))).thenReturn(Future.successful(searchResultsV2.size.toLong))
        when(mockSearchServiceV2.find(eqTo(searchRequest))).thenReturn(Source(searchResultsV2))

        val result = searchControllerV2.search("v2")(fakePostRequest(Json.toJson(searchRequest)))
        status(result) shouldBe OK

        val responseBody = contentAsString(result).split("\n").map(Json.parse).toSeq
        val expectedBody = searchResultsV2.map(searchResult => Json.toJson(searchResult)(using SearchResultV2.httpFormat))
        responseBody shouldBe expectedBody
      }
    }

    "return NoContent when search results count is 0" in new Setup {
      forAll(validSearchRequestGen) { searchRequest =>
        when(mockSearchServiceV2.count(eqTo(searchRequest))).thenReturn(Future.successful(0L))

        val result = searchControllerV2.search("v2")(fakePostRequest(Json.toJson(searchRequest)))
        status(result) shouldBe NO_CONTENT
      }
    }

    "fail validation and return NotImplemented when version is invalid" in new Setup {
      forAll(validSearchRequestGen, searchResultsV2Gen) { (searchRequest, searchResultsV2) =>

        val result = searchControllerV2.search("v3")(fakePostRequest(Json.toJson(searchRequest)))
        status(result)        shouldBe NOT_IMPLEMENTED
        contentAsString(result) should include("search version v3 not implemented")
      }
    }

    "fail with 417 when search results count exceeds max allowed" in new Setup {
      forAll(validSearchRequestGen) { searchRequest =>
        when(mockSearchServiceV2.count(eqTo(searchRequest))).thenReturn(Future.successful(eventStoreConfig.searchResultLimit + 1L))

        val result = searchControllerV2.search("v2")(fakePostRequest(Json.toJson(searchRequest)))
        status(result) shouldBe EXPECTATION_FAILED
      }
    }

    "fail validation and return BadRequest when the payload is invalid" in new Setup {
      forAll(validSearchRequestGen) { searchRequest =>
        val invalidSearchRequest = searchRequest.copy(
          dateFrom = searchRequest.dateTo.plusDays(1),
          dateTo   = searchRequest.dateFrom
        )
        val jsValue = Json.toJson(invalidSearchRequest)

        val result = searchControllerV2.search("v3")(fakePostRequest(jsValue))
        status(result)        shouldBe BAD_REQUEST
        contentAsString(result) should include("Invalid payload")
      }
    }
  }
}
