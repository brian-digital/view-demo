/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.controllers

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status
import play.api.libs.json.{Format, Json, Reads, Writes}
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.blockandsignalsgdsstore.config.EventStoreConfig
import uk.gov.hmrc.blockandsignalsgdsstore.controllers.actions.UserAgentFilter
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.EventType
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.{SearchRequest, SearchResult}
import uk.gov.hmrc.blockandsignalsgdsstore.services.SearchService
import uk.gov.hmrc.blockandsignalsgdsstore.test.Generators
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class SearchControllerSpec extends AnyWordSpec with Matchers with Generators with ScalaCheckDrivenPropertyChecks with ScalaFutures {

  trait Setup {
    implicit val system: ActorSystem = ActorSystem("TestActorSystem")
    implicit val materializer: Materializer = SystemMaterializer(system).materializer
    implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val searchResultFormat: Writes[SearchResult] = SearchResult.httpWrites
    implicit val eventTypeReads: Reads[EventType] = EventType.searchValueReads
    implicit val httpReads: Reads[SearchResult] = Json.reads[SearchResult]

    val mockSearchService: SearchService = mock[SearchService]
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
    val searchController: SearchController = new SearchController(
      Helpers.stubControllerComponents(),
      mockSearchService,
      eventStoreConfig,
      mockUserAgentFilter
    )

    def fakePostRequest[T](body: T): FakeRequest[T] = FakeRequest("POST", "/").withBody(body)

  }

  "SearchController /search/results" should {
    "return searchResults" in new Setup {
      forAll(validSearchRequestGen, searchResultsGen) { (searchRequest, searchResults) =>
        when(mockSearchService.count(eqTo(searchRequest))).thenReturn(Future.successful(searchResults.size.toLong))
        when(mockSearchService.find(eqTo(searchRequest))).thenReturn(Source(searchResults))

        val result = searchController.search()(fakePostRequest(Json.toJson(searchRequest)))
        status(result) shouldBe Status.OK

        val responseBody = contentAsString(result).split("\n").map(Json.parse).map(_.as[SearchResult])
        responseBody shouldBe searchResults
      }
    }

    "return NoContent when search results count is 0" in new Setup {
      forAll(validSearchRequestGen) { searchRequest =>

        when(mockSearchService.count(eqTo(searchRequest))).thenReturn(Future.successful(0L))

        val result = searchController.search()(fakePostRequest(Json.toJson(searchRequest)))
        status(result) shouldBe Status.NO_CONTENT

      }
    }

    "fail with 417 when search results count exceeds max allowed" in new Setup {
      forAll(validSearchRequestGen) { searchRequest =>

        when(mockSearchService.count(eqTo(searchRequest)))
          .thenReturn(Future.successful(eventStoreConfig.searchResultLimit + 1L))

        val result = searchController.search()(fakePostRequest(Json.toJson(searchRequest)))
        status(result) shouldBe Status.EXPECTATION_FAILED

      }
    }

    "fail validation and return BadRequest when the payload is invalid" in new Setup {
      forAll(validSearchRequestGen) { searchRequest =>
        val invalidSearchRequest = searchRequest.copy(
          dateFrom = searchRequest.dateTo.plusDays(1),
          dateTo   = searchRequest.dateFrom
        )
        val jsValue = Json.toJson(invalidSearchRequest)

        val result = searchController.search()(fakePostRequest(jsValue))
        status(result)        shouldBe Status.BAD_REQUEST
        contentAsString(result) should include("Invalid payload")
      }
    }
  }

  "SearchController /search/count" should {
    "return count correctly" in new Setup {
      forAll(validSearchRequestGen, Gen.posNum[Long]) { (searchRequest, count) =>
        when(mockSearchService.count(eqTo(searchRequest))).thenReturn(Future.successful(count))
        val result = searchController.count()(fakePostRequest(Json.toJson(searchRequest)))
        status(result)        shouldBe Status.OK
        contentAsJson(result) shouldBe Json.obj("count" -> count)
      }
    }

    "fail validation and return BadRequest when the payload is invalid" in new Setup {
      forAll(validSearchRequestGen) { searchRequest =>
        val invalidSearchRequest = searchRequest.copy(
          dateFrom = searchRequest.dateTo.plusDays(1),
          dateTo   = searchRequest.dateFrom
        )
        val jsValue = Json.toJson(invalidSearchRequest)

        val result = searchController.count()(fakePostRequest(jsValue))
        status(result)        shouldBe Status.BAD_REQUEST
        contentAsString(result) should include("Invalid payload")
      }
    }
  }

}
