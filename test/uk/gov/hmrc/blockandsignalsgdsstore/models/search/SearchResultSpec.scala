/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.search

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{Json, Reads, Writes}
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.EventType
import uk.gov.hmrc.blockandsignalsgdsstore.test.Generators

class SearchResultSpec extends AnyWordSpec with Matchers with Generators with ScalaCheckDrivenPropertyChecks {

  implicit val searchResultWrites: Writes[SearchResult] = SearchResult.httpWrites
  implicit val eventTypeReads: Reads[EventType] = EventType.searchValueReads
  implicit val httpReads: Reads[SearchResult] = Json.reads[SearchResult]

  "SearchResult" should {
    "successfully read from and write to JSON" in {
      forAll(searchResultGen) { searchResult =>
        val json = Json.toJson(searchResult)
        val result = json.validate[SearchResult]

        result.isSuccess shouldBe true
        result.foreach { validated =>
          validated shouldEqual searchResult
        }
      }
    }

    "successfully read and write a Seq[SearchResult] to/from JSON" in {
      forAll(Gen.listOf(searchResultGen)) { searchResultSeq =>
        val json = Json.toJson(searchResultSeq)
        val result = json.validate[Seq[SearchResult]]

        result.isSuccess shouldBe true
        result.foreach { validatedSeq =>
          validatedSeq shouldEqual searchResultSeq
        }
      }
    }

    "fail to read from invalid JSON" in {
      val invalidJson = Json.obj(
        "invalidField" -> "invalidValue"
      )
      val result = invalidJson.validate[SearchResult]
      result.isError shouldBe true
    }

  }
}
