/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.search

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.{CredentialType, InitiatingEntity, InterventionCode}
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.EventType
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

final case class SearchResult(
  // Values shared between two or more event types
  credId: Option[String],
  subjectId: Option[String],
  timeOfInterest: Instant,
  eventType: EventType,

  // Account Intervention values
  action: Option[String],

  // Account Concern values
  reason: Option[String],
  rationale: Option[String],
  initiatingEntity: Option[InitiatingEntity],

  // Credential Compromise values
  credentialType: Option[CredentialType],
  reasonAdmin: Option[String],
  reasonUser: Option[String],
  emailAddress: Option[String],
  interventionCode: Option[InterventionCode]
)

object SearchResult {

  val httpWrites: Writes[SearchResult] = {
    given Writes[EventType] = EventType.searchValueWrites
    given Writes[Instant] = Writes.DefaultInstantWrites
    Json.writes[SearchResult]
  }

  private val mongoReads: Reads[SearchResult] = (
    (__ \ "event" \ "credId").readNullable[String] ~
      (__ \ "event" \ "subjectId").readNullable[String] ~
      (__ \ "timeOfInterest").read[Instant](MongoJavatimeFormats.instantReads) ~
      (__ \ "event" \ "eventType").read[EventType](EventType.mongoReads) ~
      (__ \ "action").readNullable[String] ~
      (__ \ "event" \ "eventData" \ "reason").readNullable[String] ~
      (__ \ "event" \ "eventData" \ "rationale").readNullable[String] ~
      (__ \ "event" \ "eventData" \ "initiatingEntity").readNullable[InitiatingEntity] ~
      (__ \ "event" \ "eventData" \ "credentialType").readNullable[CredentialType] ~
      (__ \ "event" \ "eventData" \ "reasonAdmin").readNullable[String] ~
      (__ \ "event" \ "eventData" \ "reasonUser").readNullable[String] ~
      (__ \ "event" \ "eventData" \ "emailAddress").readNullable[String] ~
      (__ \ "event" \ "eventData" \ "interventionCode").readNullable[InterventionCode]
  )(SearchResult.apply)

  // this is required as part of SearchResult.mongoFormat, used in EventDocumentRepository.extraCodecs
  // both reads and writes are required for a Codec
  private val mongoWrites: Writes[SearchResult] = (
    (__ \ "event" \ "credId").writeNullable[String] ~
      (__ \ "event" \ "subjectId").writeNullable[String] ~
      (__ \ "timeOfInterest").write[Instant](MongoJavatimeFormats.instantWrites) ~
      (__ \ "event" \ "eventType").write[EventType](EventType.mongoWrites) ~
      (__ \ "action").writeNullable[String] ~
      (__ \ "event" \ "eventData" \ "reason").writeNullable[String] ~
      (__ \ "event" \ "eventData" \ "rationale").writeNullable[String] ~
      (__ \ "event" \ "eventData" \ "initiatingEntity").writeNullable[InitiatingEntity] ~
      (__ \ "event" \ "eventData" \ "credentialType").writeNullable[CredentialType] ~
      (__ \ "event" \ "eventData" \ "reasonAdmin").writeNullable[String] ~
      (__ \ "event" \ "eventData" \ "reasonUser").writeNullable[String] ~
      (__ \ "event" \ "eventData" \ "emailAddress").writeNullable[String] ~
      (__ \ "event" \ "eventData" \ "interventionCode").writeNullable[InterventionCode]
  )(Tuple.fromProductTyped(_))

  val mongoFormat: Format[SearchResult] = Format(mongoReads, mongoWrites)
}
