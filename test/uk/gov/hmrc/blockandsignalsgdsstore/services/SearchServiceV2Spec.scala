/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.services

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.blockandsignalsgdsstore.repositories.GDSSearchView
import uk.gov.hmrc.blockandsignalsgdsstore.test.Generators

import scala.concurrent.ExecutionContext.Implicits.global

class SearchServiceV2Spec extends AnyWordSpec with Matchers with ScalaFutures with Generators {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = Materializer(system)

  "SearchServiceV2" should {
    "return search results provided by GDSSearchView" in {
      val mockView = mock[GDSSearchView]
      val expectedResults = searchResultsV2Gen.sample.get
      val searchRequest = validSearchRequestGen.sample.get

      when(mockView.find(eqTo(searchRequest))).thenReturn(Source(expectedResults))

      val service = new SearchServiceV2(mockView)

      val result = service.find(searchRequest).runWith(Sink.seq)
      result.futureValue shouldBe expectedResults
    }

    "return no search results when EventDocumentRepository returns no results" in {
      val mockView = mock[GDSSearchView]
      val searchRequest = validSearchRequestGen.sample.get

      when(mockView.find(eqTo(searchRequest))).thenReturn(Source.empty)

      val service = new SearchServiceV2(mockView)

      val result = service.find(searchRequest).runWith(Sink.seq)
      result.futureValue shouldBe empty
    }

    "propagate errors from EventDocumentRepository" in {
      val mockView = mock[GDSSearchView]
      val searchRequest = validSearchRequestGen.sample.get

      when(mockView.find(eqTo(searchRequest))).thenReturn(Source.failed(new RuntimeException("Search failed")))

      val service = new SearchServiceV2(mockView)

      val result = service.find(searchRequest).runWith(Sink.seq).failed

      val thrownException = result.futureValue
      thrownException               shouldBe a[RuntimeException]
      thrownException.getMessage shouldEqual "Search failed"
    }
  }
}
