/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.common

import play.api.libs.json.*

enum InterventionCode(val code: String):
  case PasswordReset                   extends InterventionCode("04")
  case IDReverification                extends InterventionCode("05")
  case PasswordResetAndReproveIdentity extends InterventionCode("06")

object InterventionCode {

  private val reads: Reads[InterventionCode] = Reads {
    case JsString(s) =>
      values.find(_.code == s).map(JsSuccess(_)).getOrElse(JsError(s"Unknown InterventionCode: $s"))
    case _ => JsError("Expected string for InterventionCode")
  }

  private val writes: Writes[InterventionCode] = Writes(ic => JsString(ic.code.toLowerCase()))

  given format: Format[InterventionCode] = Format(reads, writes)
}
