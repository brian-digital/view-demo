/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.common

import play.api.libs.json.*

enum InitiatingEntity(val stringValue: String):
  case Admin   extends InitiatingEntity("admin")
  case User    extends InitiatingEntity("user")
  case Policy  extends InitiatingEntity("policy")
  case Analyst extends InitiatingEntity("analyst")
  case System  extends InitiatingEntity("system")

object InitiatingEntity {

  private val reads: Reads[InitiatingEntity] = Reads {
    case JsString(s) =>
      values.find(_.stringValue == s).map(JsSuccess(_)).getOrElse(JsError(s"Unknown InitiatingEntity: $s"))
    case _ => JsError("Expected string for InitiatingEntity")
  }

  private val writes: Writes[InitiatingEntity] = Writes(ie => JsString(ie.stringValue))

  given format: Format[InitiatingEntity] = Format(reads, writes)
}
