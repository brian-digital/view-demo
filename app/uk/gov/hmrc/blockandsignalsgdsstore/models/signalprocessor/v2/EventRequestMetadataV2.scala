/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v2

import play.api.libs.json.{Json, Reads}

case class EventRequestMetadataV2(signalsEventType: RequestEventTypeV2, originalEventType: String, jti: String, iat: Long)

object EventRequestMetadataV2 {
  val httpReads: Reads[EventRequestMetadataV2] = Json.reads[EventRequestMetadataV2]
}
