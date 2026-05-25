/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.search

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{Format, Json, Reads}
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.EventType
import uk.gov.hmrc.blockandsignalsgdsstore.test.Generators

class SearchResultV2Spec extends AnyWordSpec with Matchers with Generators with ScalaCheckDrivenPropertyChecks {

  implicit val eventTypeReads: Reads[EventType] = EventType.searchValueReads

  "SearchResultV2" should {

    // TODO [WD] Split into read from Mongo and write for http response

    "successfully read from and write to JSON" in {

      forAll(searchResultV2Gen) { searchResultV2 =>
        val json = Json.toJson(searchResultV2)(using SearchResultV2.mongoFormat)
        val result = json.validate[SearchResultV2](using SearchResultV2.mongoFormat)

        result.isSuccess shouldBe true
        result.foreach { validated =>
          validated shouldEqual searchResultV2
        }
      }
    }

    "successfully read and write a Seq[SearchResultV2] to/from JSON" in {
      forAll(Gen.listOf(searchResultV2Gen)) { searchResultV2Seq =>
        given Format[SearchResultV2] = SearchResultV2.mongoFormat
        val json = Json.toJson(searchResultV2Seq)
        val result = json.validate[Seq[SearchResultV2]]

        result.isSuccess shouldBe true
        result.foreach { validatedSeq =>
          validatedSeq shouldEqual searchResultV2Seq
        }
      }
    }

    "fail to read from invalid JSON" in {
      val invalidJson = Json.obj(
        "invalidField" -> "invalidValue"
      )
      val result = invalidJson.validate[SearchResultV2](using SearchResultV2.mongoFormat)
      result.isError shouldBe true
    }
  }
}
