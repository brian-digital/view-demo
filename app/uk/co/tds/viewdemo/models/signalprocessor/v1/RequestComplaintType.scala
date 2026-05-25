/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.models.signalprocessor.v1

import play.api.libs.json.*

enum RequestComplaintType(val spValue: String):
  case AccountComplaint       extends RequestComplaintType("accountComplaint")
  case AccessComplaint  extends RequestComplaintType("accessComplaint")
  case IdCompromise extends RequestComplaintType("idCompromise")

object RequestComplaintType {
  given httpReads: Reads[RequestComplaintType] = Reads {
    case JsString(s) =>
      values.find(_.spValue == s).map(JsSuccess(_)).getOrElse(JsError(s"Unknown RequestComplaintType: $s"))
    case _ => JsError("Expected string for RequestComplaintType")
  }

  given Writes[RequestComplaintType] = Writes(t => JsString(t.spValue))
}
