/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.common

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class RationaleSpec extends AnyWordSpec with Matchers {

  private val cases = List(
    "RA01" -> Rationale.RA01RuleBasedAlert,
    "RA02" -> Rationale.RA02UserReported,
    "RA03" -> Rationale.RA03OtherPersonReported,
    "RA04" -> Rationale.RA04OtherInvestigations,
    "RA05" -> Rationale.RA05CriminalInvestigations,
    "RA06" -> Rationale.RA06Intelligence,
    "RA07" -> Rationale.RA07SecurityIncident,
    "RA99" -> Rationale.RA99UnknownRationale
  )

  "Rationale" should {
    "read from JSON" in {
      cases.foreach { (input, expected) =>
        Rationale.format.reads(JsString(input)) shouldBe JsSuccess(expected)
      }
    }

    "reject invalid JSON" in {
      Rationale.format.reads(JsString("asdf")) shouldBe JsError("Unknown Rationale: asdf")
      Rationale.format.reads(JsNumber(123))    shouldBe JsError("Expected string for Rationale")
    }

    "write to JSON" in {
      val caseStrings = cases.map(_.head)
      Rationale.values.zip(caseStrings).foreach { (value, stringValue) =>
        Rationale.format.writes(value) shouldBe JsString(stringValue)
      }
    }
  }
}
