/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.search

import org.scalacheck.Gen
import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.blockandsignalsgdsstore.config.EventStoreConfig
import uk.gov.hmrc.blockandsignalsgdsstore.test.Generators

import scala.concurrent.duration.FiniteDuration

class SearchRequestSpec extends AnyWordSpec with Matchers with Generators with ScalaCheckDrivenPropertyChecks {
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

  "SearchRequest validation" should {
    "successfully validate a valid SearchRequest" in {
      forAll(validSearchRequestGen) { searchRequest =>
        val request = searchRequest.copy(credIds = Some(Seq("0")))
        val json = Json.toJson(request)
        val result = json.validate[SearchRequest]

        result.isSuccess shouldBe true
        result.foreach { validated =>
          validated shouldEqual request
        }
      }
    }

    "fail validation for invalid dates (dateFrom > dateTo)" in {
      forAll(validSearchRequestGen) { validSearchRequest =>
        val invalidSearchRequest = validSearchRequest.copy(
          dateFrom = validSearchRequest.dateTo.plusDays(1),
          dateTo   = validSearchRequest.dateFrom
        )
        val invalidJson = Json.toJson(invalidSearchRequest)
        val result = invalidJson.validate[SearchRequest]

        result.isError shouldBe true
        val errors = result.asEither.left.value.map(_._2).flatten.map(_.message)
        errors should contain("dateFrom must be equal to or before dateTo")
      }
    }

    "fail validation when credIds and subjectIds exceed max allowed combined size" in {
      forAll(
        validSearchRequestGen,
        Gen.listOfN(eventStoreConfig.searchRequestMaxAllowedCredIds, credIdGen),
        Gen.listOfN(eventStoreConfig.searchRequestMaxAllowedSubjectIds, subjectIdGen)
      ) { (validSearchRequest, credIds, subjectIds) =>
        val invalidSearchRequest = validSearchRequest.copy(
          credIds    = Some(credIds),
          subjectIds = Some(subjectIds)
        )
        val invalidJson = Json.toJson(invalidSearchRequest)
        val result = invalidJson.validate[SearchRequest]

        result.isError shouldBe true
        val errors = result.asEither.left.value.map(_._2).flatten.map(_.message)
        errors should contain(
          s"credIds and subjectIds must be less than ${eventStoreConfig.searchRequestMaxAllowedIds}"
        )
      }
    }
  }

}
