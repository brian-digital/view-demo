/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v2

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*

case class EventRequestV2(metadata: EventRequestMetadataV2, details: EventRequestDetailsV2)

object EventRequestV2 {
  val httpReads: Reads[EventRequestV2] = (
    (__ \ "metadata").read[EventRequestMetadataV2](EventRequestMetadataV2.httpReads) ~
      (__ \ "metadata")
        .read[EventRequestMetadataV2](EventRequestMetadataV2.httpReads)
        .flatMap(eventMetadata => (__ \ "details").read[EventRequestDetailsV2](EventRequestDetailsV2.httpReads(eventMetadata.signalsEventType)))
  )(EventRequestV2.apply)
}
