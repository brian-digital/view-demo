/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.common

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class InterventionCodeSpec extends AnyWordSpec with Matchers {

  private val cases = List(
    "04" -> InterventionCode.PasswordReset,
    "05" -> InterventionCode.IDReverification,
    "06" -> InterventionCode.PasswordResetAndReproveIdentity
  )

  "InterventionCode" should {
    "read from JSON" in {
      cases.foreach { (input, expected) =>
        InterventionCode.format.reads(JsString(input)) shouldBe JsSuccess(expected)
      }
    }

    "reject invalid JSON" in {
      InterventionCode.format.reads(JsString("asdf")) shouldBe JsError("Unknown InterventionCode: asdf")
      InterventionCode.format.reads(JsNumber(123))    shouldBe JsError("Expected string for InterventionCode")
    }

    "write to JSON" in {
      val caseStrings = cases.map(_.head)
      InterventionCode.values.zip(caseStrings).foreach { (value, stringValue) =>
        InterventionCode.format.writes(value) shouldBe JsString(stringValue)
      }
    }
  }
}
