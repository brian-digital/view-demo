/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v2

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, __}
import uk.gov.hmrc.blockandsignalsgdsstore.utils.TestData

class EventRequestV2Spec extends AnyWordSpec with Matchers with TestData {
  "EventRequestV2 httpReads" should {
    "accept valid JSON" when {
      "the signal is a credential concern" in {
        val json = EventRequestV2Json.credentialConcernJsonFull
        EventRequestV2.httpReads.reads(json) shouldBe JsSuccess(EventRequestV2Models.credentialConcernRequestModel)
      }

      "the signal is a device concern" in {
        val json = EventRequestV2Json.deviceConcernJsonFull
        EventRequestV2.httpReads.reads(json) shouldBe JsSuccess(EventRequestV2Models.deviceConcernRequestModel)
      }
    }

    "reject invalid JSON" when {

      def removeJsonTimestamps(jsValue: JsValue): JsValue = jsValue
        .transform(
          (__ \ "details" \ "eventTimestampMs").json.prune andThen
            (__ \ "details" \ "startTimeMs").json.prune andThen
            (__ \ "details" \ "sndTimeMs").json.prune
        )
        .get

      "the credential concern is missing all timestamps" in {
        val json = removeJsonTimestamps(EventRequestV2Json.credentialConcernJsonFull)
        val result = EventRequestV2.httpReads.reads(json)
        result                              shouldBe a[JsError]
        result.asInstanceOf[JsError].toString should include("CredentialConcernEventDetails is missing all timestamps")
      }

      "the device concern is missing all timestamps" in {
        val json = removeJsonTimestamps(EventRequestV2Json.deviceConcernJsonFull)
        val result = EventRequestV2.httpReads.reads(json)
        result                              shouldBe a[JsError]
        result.asInstanceOf[JsError].toString should include("DeviceConcernEventDetails is missing all timestamps")
      }

      "the JSON is completely wrong" in {
        EventRequestV2.httpReads.reads(Json.obj("invalid" -> "json")) shouldBe a[JsError]
      }
    }
  }
}
