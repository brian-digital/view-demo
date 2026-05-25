/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.models.common

import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Reads, Writes}

enum CredentialType(val stringValue: String):
  case Account              extends CredentialType("account")
  case CR                   extends CredentialType("CR")
  case IR                   extends CredentialType("IR")
  case DrivingPermit        extends CredentialType("drivingPermit")
  case Password             extends CredentialType("password")
  case PhoneSms             extends CredentialType("phone-sms")
  case Passport             extends CredentialType("passport")
  case Email                extends CredentialType("email")
  case Unknown              extends CredentialType("unknown")
  case Authenticator        extends CredentialType("authenticator")
  case VerifiableCredential extends CredentialType("verifiable-credential")
  case Pin                  extends CredentialType("pin")
  case X509                 extends CredentialType("X509")
  case Fido2Platform        extends CredentialType("fido2-platform")
  case Fido2Roaming         extends CredentialType("fido2-roaming")
  case FidoU2f              extends CredentialType("fido-u2f")
  case PhoneVoice           extends CredentialType("phone-voice")
  case App                  extends CredentialType("app")
  case Nino                 extends CredentialType("NINO")
  case Fwp                  extends CredentialType("FWP")

object CredentialType {
  private val reads: Reads[CredentialType] = Reads {
    case JsString(s) =>
      values.find(_.stringValue == s).map(JsSuccess(_)).getOrElse(JsError(s"Unknown CredentialType: $s"))
    case _ => JsError("Expected string for CredentialType")
  }

  private val writes: Writes[CredentialType] = Writes(ic => JsString(ic.stringValue))

  given format: Format[CredentialType] = Format(reads, writes)
}
