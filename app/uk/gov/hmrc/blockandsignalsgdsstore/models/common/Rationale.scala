/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.common

import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Json, Reads, Writes}

enum Rationale(val code: String):
  case RA01RuleBasedAlert         extends Rationale("RA01")
  case RA02UserReported           extends Rationale("RA02")
  case RA03OtherPersonReported    extends Rationale("RA03")
  case RA04OtherInvestigations    extends Rationale("RA04")
  case RA05CriminalInvestigations extends Rationale("RA05")
  case RA06Intelligence           extends Rationale("RA06")
  case RA07SecurityIncident       extends Rationale("RA07")
  case RA99UnknownRationale       extends Rationale("RA99")

object Rationale {
  private val reads: Reads[Rationale] = Reads {
    case JsString(s) =>
      values.find(_.code == s).map(JsSuccess(_)).getOrElse(JsError(s"Unknown Rationale: $s"))
    case _ => JsError("Expected string for Rationale")
  }

  private val writes: Writes[Rationale] = Writes(ic => JsString(ic.code))

  given format: Format[Rationale] = Format(reads, writes)
}
