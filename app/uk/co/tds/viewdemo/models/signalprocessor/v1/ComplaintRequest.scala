/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.models.signalprocessor.v1

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Clock, Instant}

case class ComplaintRequest(
                             metadata: ComplaintRequestMetadata,
                             details: ComplaintRequestDetails,
                             storedAt: Instant)

object ComplaintRequest {


  val httpReads: Reads[ComplaintRequest] = (
    (__ \ "metadata").read[ComplaintRequestMetadata](ComplaintRequestMetadata.httpReads) ~
      (__ \ "metadata")
        .read[ComplaintRequestMetadata](ComplaintRequestMetadata.httpReads)
        .flatMap(eventMetadata => (__ \ "details").read[ComplaintRequestDetails](ComplaintRequestDetails.httpReads(eventMetadata.complaintType)))
  )(ComplaintRequest.apply(_, _, Instant.now(Clock.systemUTC())))


  val mongoWrites: Writes[ComplaintRequest] = {
    given Writes[ComplaintRequestMetadata] = ComplaintRequestMetadata.httpWrites
    given Writes[ComplaintRequestDetails] = ComplaintRequestDetails.mongoWrites
    given Format[Instant] = MongoJavatimeFormats.instantFormat
    Json.writes[ComplaintRequest]
  }

  val mongoFormat: Format[ComplaintRequest] = Format(httpReads, mongoWrites)
}
