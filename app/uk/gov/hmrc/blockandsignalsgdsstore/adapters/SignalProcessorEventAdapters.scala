/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.adapters

import uk.gov.hmrc.blockandsignalsgdsstore.models.db.*
import uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v1.*
import uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v2.*

import java.time.{Clock, Instant}
import javax.inject.Inject

object SignalProcessorEventAdapters {

  object V1 {
    private def toDbEventType(eventType: RequestEventTypeV1): EventType =
      eventType match
        case RequestEventTypeV1.AccountConcern       => EventType.AccountConcern
        case RequestEventTypeV1.AccountIntervention  => EventType.AccountIntervention
        case RequestEventTypeV1.CredentialCompromise => EventType.CredentialCompromise

    private def getHmrcAccountId(eventRequestDetails: EventRequestDetailsV1): Option[String] =
      eventRequestDetails match
        case erd: AccountConcernEventDetails       => Some(erd.credId)
        case erd: AccountInterventionEventDetails  => Some(erd.credId)
        case erd: CredentialCompromiseEventDetails => Some(erd.credId)

    private def getOneLoginAccountId(eventRequestDetails: EventRequestDetailsV1): String =
      eventRequestDetails match
        case erd: AccountConcernEventDetails       => erd.subjectId
        case erd: AccountInterventionEventDetails  => erd.subjectId
        case erd: CredentialCompromiseEventDetails => erd.subjectId

    private def createAccountConcernEventData(erd: AccountConcernEventDetails): AccountConcernEventData =
      AccountConcernEventData(
        initiatingEntity = erd.initiatingEntity,
        reason           = erd.reason,
        rationale        = erd.rationale,
        eventTimestampMs = erd.eventTimestampMs,
        startTimeMs      = erd.startTimeMs,
        endTimeMs        = erd.endTimeMs
      )

    private def createAccountInterventionEventData(erd: AccountInterventionEventDetails): AccountInterventionEventData =
      AccountInterventionEventData(
        initiatingEntity = erd.initiatingEntity,
        state            = erd.state,
        action           = erd.action,
        eventTimestampMs = erd.eventTimestampMs
      )

    private def createCredentialCompromiseEventData(erd: CredentialCompromiseEventDetails): CredentialCompromiseEventData =
      CredentialCompromiseEventData(
        initiatingEntity = erd.initiatingEntity,
        credentialType   = erd.credentialType,
        eventTimestampMs = erd.eventTimestampMs,
        reasonAdmin      = erd.reasonAdmin,
        reasonUser       = erd.reasonUser,
        emailAddress     = erd.emailAddress,
        interventionCode = erd.interventionCode
      )

    private def createEventData(eventRequestDetails: EventRequestDetailsV1): EventData =
      eventRequestDetails match
        case erd: AccountConcernEventDetails       => createAccountConcernEventData(erd)
        case erd: AccountInterventionEventDetails  => createAccountInterventionEventData(erd)
        case erd: CredentialCompromiseEventDetails => createCredentialCompromiseEventData(erd)

    def eventRequestToDbEvent(eventRequest: EventRequestV1): Event = {
      val eventType = toDbEventType(eventRequest.metadata.signalsEventType)
      val hmrcAccountId = getHmrcAccountId(eventRequest.details)
      val oneLoginAccountId = getOneLoginAccountId(eventRequest.details)
      val eventData = createEventData(eventRequest.details)

      Event(
        eventType         = eventType,
        originalEventType = eventRequest.metadata.originalEventType,
        eventId           = eventRequest.metadata.jti,
        generatedAt       = eventRequest.metadata.iat,
        subjectId         = Some(oneLoginAccountId),
        credId            = hmrcAccountId,
        eventData         = eventData
      )
    }
  }

  object V2 {
    private def toDbEventType(eventType: RequestEventTypeV2): EventType =
      eventType match
        case RequestEventTypeV2.CredentialConcern => EventType.CredentialConcern
        case RequestEventTypeV2.DeviceConcern     => EventType.DeviceConcern

    private def getHmrcAccountId(eventRequestDetails: EventRequestDetailsV2): Option[String] =
      eventRequestDetails match
        case erd: CredentialConcernEventDetails => erd.credId
        case erd: DeviceConcernEventDetails     => erd.credId

    private def getOneLoginAccountId(eventRequestDetails: EventRequestDetailsV2): String =
      eventRequestDetails match
        case erd: CredentialConcernEventDetails => erd.accountIdentifierUri
        case erd: DeviceConcernEventDetails     => erd.accountIdentifierUri

    private def createCredentialConcernEventData(erd: CredentialConcernEventDetails): CredentialConcernEventData =
      CredentialConcernEventData(
        credentialType   = erd.credentialType,
        initiatingEntity = erd.initiatingEntity,
        reasonAdmin      = erd.reasonAdmin,
        rationale        = erd.rationale,
        eventTimestampMs = erd.eventTimestampMs,
        startTimeMs      = erd.startTimeMs,
        endTimeMs        = erd.endTimeMs,
        identifierFormat = erd.identifierFormat,
        documentNumber   = erd.documentNumber,
        expiryDate       = erd.expiryDate,
        icaoIssuerCode   = erd.icaoIssuerCode,
        personalNumber   = erd.personalNumber,
        issueNumber      = erd.issueNumber,
        issuedBy         = erd.issuedBy,
        email            = erd.email,
        phoneNumber      = erd.phoneNumber,
        nino             = erd.nino,
        sourceType       = erd.sourceType,
        sourceTypeUri    = erd.sourceTypeUri
      )

    private def createDeviceConcernEventData(erd: DeviceConcernEventDetails): DeviceConcernEventData =
      DeviceConcernEventData(
        initiatingEntity = erd.initiatingEntity,
        reasonAdmin      = erd.reasonAdmin,
        rationale        = erd.rationale,
        eventTimestampMs = erd.eventTimestampMs,
        startTimeMs      = erd.startTimeMs,
        endTimeMs        = erd.endTimeMs,
        iss              = erd.iss,
        sub              = erd.sub,
        identifiers      = erd.identifiers.getOrElse(List.empty),
        sourceType       = erd.sourceType,
        sourceTypeUri    = erd.sourceTypeUri
      )

    private def createEventData(eventRequestDetails: EventRequestDetailsV2): EventData =
      eventRequestDetails match
        case erd: CredentialConcernEventDetails => createCredentialConcernEventData(erd)
        case erd: DeviceConcernEventDetails     => createDeviceConcernEventData(erd)

    def eventRequestToDbEvent(eventRequest: EventRequestV2): Event = {
      val eventType = toDbEventType(eventRequest.metadata.signalsEventType)
      val hmrcAccountId = getHmrcAccountId(eventRequest.details)
      val oneLoginAccountId = getOneLoginAccountId(eventRequest.details)
      val eventData = createEventData(eventRequest.details)

      Event(
        eventType         = eventType,
        originalEventType = eventRequest.metadata.originalEventType,
        eventId           = eventRequest.metadata.jti,
        generatedAt       = eventRequest.metadata.iat,
        subjectId         = Some(oneLoginAccountId),
        credId            = hmrcAccountId,
        eventData         = eventData
      )
    }
  }

  def determineTimeOfInterest(eventTimestampMs: Option[Long], startTimeMs: Option[Long], endTimeMs: Option[Long]): Either[String, Instant] = {
    val timestampOption = eventTimestampMs.orElse(endTimeMs).orElse(startTimeMs)
    timestampOption match
      case Some(timestamp) => Right(Instant.ofEpochMilli(timestamp))
      case None            => Left("EventData missing time of interest")
  }

  private def getTimeOfInterest(eventData: EventData): Either[String, Instant] =
    eventData match
      case ed: AccountConcernEventData =>
        determineTimeOfInterest(ed.eventTimestampMs, ed.startTimeMs, ed.endTimeMs)
      case ed: AccountInterventionEventData =>
        Right(Instant.ofEpochMilli(ed.eventTimestampMs))
      case ed: CredentialCompromiseEventData =>
        Right(Instant.ofEpochMilli(ed.eventTimestampMs))
      case ed: CredentialConcernEventData =>
        determineTimeOfInterest(ed.eventTimestampMs, ed.startTimeMs, ed.endTimeMs)
      case ed: DeviceConcernEventData =>
        determineTimeOfInterest(ed.eventTimestampMs, ed.startTimeMs, ed.endTimeMs)

  // This is only for Account Intervention signals
  def getAction(eventData: EventData): Option[String] =
    eventData match
      case ed: AccountInterventionEventData =>
        ed.state match
          case Some("active")                                        => ed.action
          case Some(state @ ("suspended" | "permanently_suspended")) => Some(state)
          case _                                                     => None
      case _ => None

  def eventToEventDocument(event: Event)(using clock: Clock): Either[String, EventDocument] =
    getTimeOfInterest(event.eventData).map { timeOfInterest =>
      val actionOption = getAction(event.eventData)
      val storedAt: Instant = Instant.now(clock)
      EventDocument(event, storedAt, timeOfInterest, actionOption)
    }
}
