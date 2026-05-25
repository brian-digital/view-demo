/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v2

import play.api.libs.json.{JsError, JsString, JsSuccess, Reads}

/** Represents the 'signals event type' sent by signal-processor
  * @param spValue
  *   The string format used by signal-processor
  */
enum RequestEventTypeV2(val spValue: String):
  case CredentialConcern extends RequestEventTypeV2("credentialConcern")
  case DeviceConcern     extends RequestEventTypeV2("deviceConcern")

object RequestEventTypeV2 {
  given httpReads: Reads[RequestEventTypeV2] = Reads {
    case JsString(s) =>
      values.find(_.spValue == s).map(JsSuccess(_)).getOrElse(JsError(s"Unknown RequestEventTypeV2: $s"))
    case _ => JsError("Expected string for RequestEventTypeV2")
  }
}
