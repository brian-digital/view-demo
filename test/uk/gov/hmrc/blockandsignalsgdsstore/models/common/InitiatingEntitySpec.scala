/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.common

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class InitiatingEntitySpec extends AnyWordSpec with Matchers {

  private val cases = List(
    "admin"   -> InitiatingEntity.Admin,
    "user"    -> InitiatingEntity.User,
    "policy"  -> InitiatingEntity.Policy,
    "analyst" -> InitiatingEntity.Analyst,
    "system"  -> InitiatingEntity.System
  )

  "InitiatingEntity" should {
    "read from JSON" in {
      cases.foreach { (input, expected) =>
        InitiatingEntity.format.reads(JsString(input)) shouldBe JsSuccess(expected)
      }
    }

    "reject invalid JSON" in {
      InitiatingEntity.format.reads(JsString("asdf")) shouldBe JsError("Unknown InitiatingEntity: asdf")
      InitiatingEntity.format.reads(JsNumber(123))    shouldBe JsError("Expected string for InitiatingEntity")
    }

    "write to JSON" in {
      val caseStrings = cases.map(_.head)
      InitiatingEntity.values.zip(caseStrings).foreach { (value, stringValue) =>
        InitiatingEntity.format.writes(value) shouldBe JsString(stringValue)
      }
    }
  }
}
