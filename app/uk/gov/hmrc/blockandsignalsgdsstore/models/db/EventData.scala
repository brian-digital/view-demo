/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.db

import play.api.libs.json.{Json, Reads, Writes}
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.{CredentialType, DeviceConcernIdentifier, InitiatingEntity, InterventionCode, Rationale, Reason, SourceType}

sealed trait EventData

object EventData {

  private val accountConcernReadsWidened = AccountConcernEventData.mongoReads.widen[EventData]
  private val accountInterventionReadsWidened = AccountInterventionEventData.mongoReads.widen[EventData]
  private val credentialCompromiseReadsWidened = CredentialCompromiseEventData.mongoReads.widen[EventData]
  private val credentialConcernReadsWidened = CredentialConcernEventData.mongoReads.widen[EventData]
  private val deviceConcernReadsWidened = DeviceConcernEventData.mongoReads.widen[EventData]

  def mongoReads(eventType: EventType): Reads[EventData] = eventType match {
    case EventType.AccountConcern       => accountConcernReadsWidened
    case EventType.AccountIntervention  => accountInterventionReadsWidened
    case EventType.CredentialCompromise => credentialCompromiseReadsWidened
    case EventType.CredentialConcern    => credentialConcernReadsWidened
    case EventType.DeviceConcern        => deviceConcernReadsWidened
  }

  val mongoWrites: Writes[EventData] = Writes {
    case ed: AccountConcernEventData       => AccountConcernEventData.mongoWrites.writes(ed)
    case ed: AccountInterventionEventData  => AccountInterventionEventData.mongoWrites.writes(ed)
    case ed: CredentialCompromiseEventData => CredentialCompromiseEventData.mongoWrites.writes(ed)
    case ed: CredentialConcernEventData    => CredentialConcernEventData.mongoWrites.writes(ed)
    case ed: DeviceConcernEventData        => DeviceConcernEventData.mongoWrites.writes(ed)
  }
}

case class AccountConcernEventData(initiatingEntity: Option[InitiatingEntity],
                                   reason: Option[String],
                                   rationale: Option[String],
                                   eventTimestampMs: Option[Long],
                                   startTimeMs: Option[Long],
                                   endTimeMs: Option[Long]
                                  )
    extends EventData

object AccountConcernEventData {
  val mongoReads: Reads[AccountConcernEventData] = Json.reads[AccountConcernEventData]
  val mongoWrites: Writes[AccountConcernEventData] = Json.writes[AccountConcernEventData]
}

final case class AccountInterventionEventData(initiatingEntity: Option[InitiatingEntity], state: Option[String], action: Option[String], eventTimestampMs: Long) extends EventData

object AccountInterventionEventData {
  val mongoReads: Reads[AccountInterventionEventData] = Json.reads[AccountInterventionEventData]
  val mongoWrites: Writes[AccountInterventionEventData] = Json.writes[AccountInterventionEventData]
}

final case class CredentialCompromiseEventData(initiatingEntity: Option[InitiatingEntity],
                                               credentialType: Option[CredentialType],
                                               eventTimestampMs: Long,
                                               // TODO [WD] Signal processor appears to have no validation on both
                                               //  reasonAdmin and reasonUser for credential compromise, therefore
                                               //  we must use String?
                                               reasonAdmin: Option[String],
                                               reasonUser: Option[String],
                                               emailAddress: Option[String],
                                               interventionCode: Option[InterventionCode]
                                              )
    extends EventData

object CredentialCompromiseEventData {
  val mongoReads: Reads[CredentialCompromiseEventData] = Json.reads[CredentialCompromiseEventData]
  val mongoWrites: Writes[CredentialCompromiseEventData] = Json.writes[CredentialCompromiseEventData]
}

final case class CredentialConcernEventData(credentialType: Option[CredentialType],
                                            initiatingEntity: InitiatingEntity,
                                            reasonAdmin: Reason,
                                            rationale: Rationale,
                                            eventTimestampMs: Option[Long],
                                            startTimeMs: Option[Long],
                                            endTimeMs: Option[Long],
                                            identifierFormat: String,
                                            documentNumber: Option[String],
                                            expiryDate: Option[String],
                                            icaoIssuerCode: Option[String],
                                            personalNumber: Option[String],
                                            issueNumber: Option[String],
                                            issuedBy: Option[String],
                                            email: Option[String],
                                            phoneNumber: Option[String],
                                            nino: Option[String],
                                            sourceType: Option[SourceType],
                                            sourceTypeUri: Option[String]
                                           )
    extends EventData

object CredentialConcernEventData {
  val mongoReads: Reads[CredentialConcernEventData] = Json.reads[CredentialConcernEventData]
  val mongoWrites: Writes[CredentialConcernEventData] = Json.writes[CredentialConcernEventData]
}

final case class DeviceConcernEventData(initiatingEntity: InitiatingEntity,
                                        reasonAdmin: Reason,
                                        rationale: Rationale,
                                        eventTimestampMs: Option[Long],
                                        startTimeMs: Option[Long],
                                        endTimeMs: Option[Long],
                                        iss: Option[String],
                                        sub: Option[String],
                                        identifiers: List[DeviceConcernIdentifier],
                                        sourceType: Option[SourceType],
                                        sourceTypeUri: Option[String]
                                       )
    extends EventData

object DeviceConcernEventData {
  val mongoReads: Reads[DeviceConcernEventData] = Json.reads[DeviceConcernEventData]
  val mongoWrites: Writes[DeviceConcernEventData] = Json.writes[DeviceConcernEventData]
}
