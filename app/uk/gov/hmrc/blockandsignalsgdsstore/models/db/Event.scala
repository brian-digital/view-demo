/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.db

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*

final case class Event(
  eventType: EventType,
  originalEventType: String,
  eventId: String,
  generatedAt: Long,
  subjectId: Option[String],
  credId: Option[String],
  eventData: EventData
)

object Event {

  private val mongoReads: Reads[Event] = (
    (__ \ "eventType").read[EventType](EventType.mongoReads) ~
      (__ \ "originalEventType").read[String] ~
      (__ \ "eventId").read[String] ~
      (__ \ "generatedAt").read[Long] ~
      (__ \ "subjectId").readNullable[String] ~
      (__ \ "credId").readNullable[String] ~
      (__ \ "eventType").read[EventType](EventType.mongoReads).flatMap { et =>
        (__ \ "eventData").read(EventData.mongoReads(et))
      }
  )(Event.apply)

  private val mongoWrites: Writes[Event] = (
    (__ \ "eventType").write[EventType](EventType.mongoWrites) ~
      (__ \ "originalEventType").write[String] ~
      (__ \ "eventId").write[String] ~
      (__ \ "generatedAt").write[Long] ~
      (__ \ "subjectId").writeNullable[String] ~
      (__ \ "credId").writeNullable[String] ~
      (__ \ "eventData").write[EventData](EventData.mongoWrites)
  )(Tuple.fromProductTyped(_))

  given mongoFormat: Format[Event] = Format(mongoReads, mongoWrites)
}
