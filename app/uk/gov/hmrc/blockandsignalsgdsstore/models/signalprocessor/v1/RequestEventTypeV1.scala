/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v1

import play.api.libs.json.*

/** Represents the 'signals event type' sent by signal-processor
  * @param spValue
  *   The string format used by signal-processor
  */
enum RequestEventTypeV1(val spValue: String):
  case AccountConcern       extends RequestEventTypeV1("accountConcern")
  case AccountIntervention  extends RequestEventTypeV1("accountIntervention")
  case CredentialCompromise extends RequestEventTypeV1("credential-compromise")

object RequestEventTypeV1 {
  given httpReads: Reads[RequestEventTypeV1] = Reads {
    case JsString(s) =>
      values.find(_.spValue == s).map(JsSuccess(_)).getOrElse(JsError(s"Unknown RequestEventTypeV1: $s"))
    case _ => JsError("Expected string for RequestEventTypeV1")
  }
}
