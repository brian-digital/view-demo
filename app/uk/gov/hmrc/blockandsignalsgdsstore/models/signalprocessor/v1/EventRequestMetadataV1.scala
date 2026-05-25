/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v1

import play.api.libs.json.{Json, Reads}

case class EventRequestMetadataV1(signalsEventType: RequestEventTypeV1, originalEventType: String, jti: String, iat: Long)

object EventRequestMetadataV1 {
  val httpReads: Reads[EventRequestMetadataV1] = Json.reads[EventRequestMetadataV1]
}
