/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v1

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsNumber, JsString, JsSuccess}

class RequestEventTypeV1Spec extends AnyWordSpec with Matchers {

  private val cases = List(
    "accountConcern"        -> RequestEventTypeV1.AccountConcern,
    "accountIntervention"   -> RequestEventTypeV1.AccountIntervention,
    "credential-compromise" -> RequestEventTypeV1.CredentialCompromise
  )

  "RequestEventTypeV1" should {
    "read valid JSON" in {
      cases.foreach { (input, expected) =>
        RequestEventTypeV1.httpReads.reads(JsString(input)) shouldBe JsSuccess(expected)
      }
    }

    "reject invalid JSON" in {
      RequestEventTypeV1.httpReads.reads(JsString("asdf")) shouldBe JsError("Unknown RequestEventTypeV1: asdf")
      RequestEventTypeV1.httpReads.reads(JsNumber(123))    shouldBe JsError("Expected string for RequestEventTypeV1")
    }
  }
}
