/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.common

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class DeviceConcernIdentifierSpec extends AnyWordSpec with Matchers {

  private val cases = List(
    "device-hash"            -> DeviceConcernIdentifierType.DeviceHash,
    "device-id"              -> DeviceConcernIdentifierType.DeviceId,
    "journey-id"             -> DeviceConcernIdentifierType.JourneyId,
    "persistent-session-id"  -> DeviceConcernIdentifierType.PersistentSessionId,
    "session-id"             -> DeviceConcernIdentifierType.SessionId,
    "user-device-ip-address" -> DeviceConcernIdentifierType.UserDeviceIpAddress
  )

  "DeviceConcernIdentifier" should {
    "read from JSON" in {
      cases.foreach { (formatString, expectedFormat) =>
        val input = Json.obj("format" -> JsString(formatString), "value" -> JsString("test"))
        val expected = DeviceConcernIdentifier(expectedFormat, "test")
        DeviceConcernIdentifier.reads.reads(input) shouldBe JsSuccess(expected)
      }
    }

    "reject invalid JSON" in {
      val invalidObject1 = Json.obj("format" -> JsString("asdf"), "value" -> JsString("test"))
      val invalidObject2 = Json.obj("format" -> JsNumber(123), "value" -> JsString("test"))
      val invalidString = JsString("asdf")
      DeviceConcernIdentifier.reads.reads(invalidObject1) shouldBe a[JsError]
      DeviceConcernIdentifier.reads.reads(invalidObject2) shouldBe a[JsError]
      DeviceConcernIdentifier.reads.reads(invalidString)  shouldBe a[JsError]
    }

    "write to JSON" in {
      val caseStrings = cases.map(_.head)
      DeviceConcernIdentifierType.values.zip(caseStrings).foreach { (formatInput, formatString) =>
        val input = DeviceConcernIdentifier(formatInput, "test")
        DeviceConcernIdentifier.writes.writes(input) shouldBe Json.obj("format" -> formatString, "value" -> "test")
      }
    }
  }
}
