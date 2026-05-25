/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.common

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsNumber, JsString, JsSuccess}

class CredentialTypeSpec extends AnyWordSpec with Matchers {

  private val cases = List(
    "account"               -> CredentialType.Account,
    "CR"                    -> CredentialType.CR,
    "IR"                    -> CredentialType.IR,
    "drivingPermit"         -> CredentialType.DrivingPermit,
    "password"              -> CredentialType.Password,
    "phone-sms"             -> CredentialType.PhoneSms,
    "passport"              -> CredentialType.Passport,
    "email"                 -> CredentialType.Email,
    "unknown"               -> CredentialType.Unknown,
    "authenticator"         -> CredentialType.Authenticator,
    "verifiable-credential" -> CredentialType.VerifiableCredential,
    "pin"                   -> CredentialType.Pin,
    "X509"                  -> CredentialType.X509,
    "fido2-platform"        -> CredentialType.Fido2Platform,
    "fido2-roaming"         -> CredentialType.Fido2Roaming,
    "fido-u2f"              -> CredentialType.FidoU2f,
    "phone-voice"           -> CredentialType.PhoneVoice,
    "app"                   -> CredentialType.App,
    "NINO"                  -> CredentialType.Nino,
    "FWP"                   -> CredentialType.Fwp
  )

  "CredentialType" should {
    "read valid JSON" in {
      cases.foreach { (input, expected) =>
        CredentialType.format.reads(JsString(input)) shouldBe JsSuccess(expected)
      }
    }

    "reject invalid JSON" in {
      CredentialType.format.reads(JsString("asdf")) shouldBe JsError("Unknown CredentialType: asdf")
      CredentialType.format.reads(JsNumber(123))    shouldBe JsError("Expected string for CredentialType")
    }

    "write to JSON" in {
      val caseStrings = cases.map(_.head)
      CredentialType.values.zip(caseStrings).foreach { (value, stringValue) =>
        CredentialType.format.writes(value) shouldBe JsString(stringValue)
      }
    }
  }
}
