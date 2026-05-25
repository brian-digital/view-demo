/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.db

import play.api.libs.json.{Format, Json, Reads, Writes}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import java.time.format.DateTimeFormatter

final case class EventDocument(event: Event, storedAt: Instant, timeOfInterest: Instant, action: Option[String])

object EventDocument {

  val mongoFormat: Format[EventDocument] = {
    given Format[Instant] = MongoJavatimeFormats.instantFormat
    Json.format[EventDocument]
  }

  def maskId(id: Option[String]): String = {
    id match
      case Some(value) =>
        if (value.isEmpty) {
          "empty"
        } else if (value.length < 4) {
          "*" * value.length
        } else {
          val nMasked = {
            if (value.length >= 8) value.length - 4
            else value.length - (value.length - 4)
          }
          val (part1, part2) = value.splitAt(nMasked)
          val maskedPart = "*" * part1.length
          s"$maskedPart$part2"
        }
      case None => "empty"
  }

  def toLogString(eventDocument: EventDocument): String = {
    val maskedCredId = maskId(eventDocument.event.credId) match {
      case ""     => "empty"
      case masked => masked
    }
    val maskedSubId = maskId(eventDocument.event.subjectId)
    val eventId = eventDocument.event.eventId
    val action = eventDocument.action.getOrElse("no-action")
    val eventType = eventDocument.event.eventType.mongoValue
    val eventData = eventDocument.event.eventData
    val timeOfInterest = DateTimeFormatter.ISO_INSTANT.format(eventDocument.timeOfInterest)
    s"credId: $maskedCredId, subId: $maskedSubId, eventId: $eventId, action: $action, eventType: $eventType, eventData: $eventData, timeOfInterest: $timeOfInterest"
  }
}
