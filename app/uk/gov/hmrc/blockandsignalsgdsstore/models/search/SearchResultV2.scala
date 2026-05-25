/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.search

import play.api.libs.json.{Format, Json, Reads, Writes}
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.InitiatingEntity
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.EventType
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class SearchResultV2(
  eventType: EventType,
  hmrcCredentialId: Option[String],
  oneLoginSubjectId: Option[String],
  submittedOn: Instant,
  submittedBy: Option[InitiatingEntity],
  reason: Option[String],
  rationale: Option[String],
  accountInterventionState: Option[String],
  accountInterventionAction: Option[String],
  credentialCompromiseEmailAddress: Option[String],
  credentialCompromiseInterventionCode: Option[String],
  credentialConcernSourceType: Option[String],
  credentialConcernSourceUri: Option[String],
  credentialConcernCredentialType: Option[String],
  credentialConcernIdentifierFormat: Option[String],
  credentialConcernDocumentNumber: Option[String],
  credentialConcernExpiryDate: Option[String],
  credentialConcernIcaoIssuerCode: Option[String],
  credentialConcernPersonalNumber: Option[String],
  credentialConcernIssueNumber: Option[String],
  credentialConcernIssuedBy: Option[String],
  credentialConcernIssuingCountry: Option[String],
  credentialConcernEmailAddress: Option[String],
  credentialConcernTelephoneNumber: Option[String],
  credentialConcernNino: Option[String],
  deviceConcernSourceType: Option[String],
  deviceConcernSourceUri: Option[String],
  deviceConcernDeviceHash: Option[String],
  deviceCookieJourneyId: Option[String],
  deviceConcernPersistentSessionCookie: Option[String],
  deviceConcernDeviceId: Option[String],
  deviceConcernSessionId: Option[String],
  deviceConcernIpAddress: Option[String]
)

object SearchResultV2 {

  val mongoFormat: Format[SearchResultV2] = {
    given Format[Instant] = MongoJavatimeFormats.instantFormat
    given Format[EventType] = EventType.mongoFormat
    Json.format[SearchResultV2]
  }

  val httpFormat: Format[SearchResultV2] = {
    given Format[Instant] = Format(Reads.DefaultInstantReads, Writes.DefaultInstantWrites)
    given Format[EventType] = Format(EventType.searchValueReads, EventType.searchValueWrites)
    Json.format[SearchResultV2]
  }
}
