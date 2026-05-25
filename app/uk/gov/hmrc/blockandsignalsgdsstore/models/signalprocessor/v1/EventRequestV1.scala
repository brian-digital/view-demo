/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v1

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*

case class EventRequestV1(metadata: EventRequestMetadataV1, details: EventRequestDetailsV1)

object EventRequestV1 {
  val httpReads: Reads[EventRequestV1] = (
    (__ \ "metadata").read[EventRequestMetadataV1](EventRequestMetadataV1.httpReads) ~
      (__ \ "metadata")
        .read[EventRequestMetadataV1](EventRequestMetadataV1.httpReads)
        .flatMap(eventMetadata => (__ \ "details").read[EventRequestDetailsV1](EventRequestDetailsV1.httpReads(eventMetadata.signalsEventType)))
  )(EventRequestV1.apply)
}
