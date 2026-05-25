/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.models.signalprocessor.v1

import play.api.libs.json.{Json, Reads, Writes}

case class ComplaintRequestMetadata(complaintType: RequestComplaintType, originalComplaintType: String, complaintRef: String, complaintId: Long)

object ComplaintRequestMetadata {
  val httpReads: Reads[ComplaintRequestMetadata] = Json.reads[ComplaintRequestMetadata]
  val httpWrites: Writes[ComplaintRequestMetadata] = Json.writes[ComplaintRequestMetadata]
}
