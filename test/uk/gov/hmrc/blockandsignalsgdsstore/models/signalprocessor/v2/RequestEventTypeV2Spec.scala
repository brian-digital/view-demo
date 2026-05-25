/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v2

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsNumber, JsString, JsSuccess}

class RequestEventTypeV2Spec extends AnyWordSpec with Matchers {

  private val cases = List(
    "credentialConcern" -> RequestEventTypeV2.CredentialConcern,
    "deviceConcern"     -> RequestEventTypeV2.DeviceConcern
  )

  "RequestEventTypeV2" should {
    "read valid JSON" in {
      cases.foreach { (input, expected) =>
        RequestEventTypeV2.httpReads.reads(JsString(input)) shouldBe JsSuccess(expected)
      }
    }

    "reject invalid JSON" in {
      RequestEventTypeV2.httpReads.reads(JsString("asdf")) shouldBe JsError("Unknown RequestEventTypeV2: asdf")
      RequestEventTypeV2.httpReads.reads(JsNumber(123))    shouldBe JsError("Expected string for RequestEventTypeV2")
    }
  }
}
