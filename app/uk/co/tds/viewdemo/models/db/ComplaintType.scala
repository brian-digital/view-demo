/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.models.db

import play.api.libs.json.*

enum ComplaintType(val searchValue: String, val mongoValue: String) {
  case AccountComplaint  extends ComplaintType("account-complaint", "accountComplaint")
  case AccessComplaint   extends ComplaintType("access-complaint", "accessComplaint")
  case IdCompromise      extends ComplaintType("id-compromise", "idCompromise")
}

object ComplaintType {

  private def createReads(selector: ComplaintType => String)(str: String): JsResult[ComplaintType] =
    ComplaintType.values
      .find(selector(_) == str)
      .map(JsSuccess(_))
      .getOrElse(JsError(s"Invalid eventType: $str"))

  val mongoReads: Reads[ComplaintType] =
    Reads.of[String] flatMapResult createReads(_.mongoValue)

  val mongoWrites: Writes[ComplaintType] =
    Writes(eventType => JsString(eventType.mongoValue))

  val mongoFormat: Format[ComplaintType] = Format(
    mongoReads,
    mongoWrites
  )

  val searchValueReads: Reads[ComplaintType] =
    Reads.of[String] flatMapResult createReads(_.searchValue)

  val searchValueWrites: Writes[ComplaintType] =
    Writes(eventType => JsString(eventType.searchValue))
}
