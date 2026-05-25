/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v1

import play.api.libs.json.*
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.*

sealed trait EventRequestDetailsV1

object EventRequestDetailsV1 {

  private val accountConcernReadsWidened = AccountConcernEventDetails.httpReads.widen[EventRequestDetailsV1]
  private val accountInterventionReadsWidened = AccountInterventionEventDetails.httpReads.widen[EventRequestDetailsV1]
  private val credentialCompromiseReadsWidened = CredentialCompromiseEventDetails.httpReads.widen[EventRequestDetailsV1]

  def httpReads(eventType: RequestEventTypeV1): Reads[EventRequestDetailsV1] = {
    eventType match
      case RequestEventTypeV1.AccountConcern       => accountConcernReadsWidened
      case RequestEventTypeV1.AccountIntervention  => accountInterventionReadsWidened
      case RequestEventTypeV1.CredentialCompromise => credentialCompromiseReadsWidened
  }
}

case class AccountConcernEventDetails(subjectId: String,
                                      credId: String,
                                      initiatingEntity: Option[InitiatingEntity],
                                      reason: Option[String],
                                      rationale: Option[String],
                                      eventTimestampMs: Option[Long],
                                      startTimeMs: Option[Long],
                                      endTimeMs: Option[Long]
                                     )
    extends EventRequestDetailsV1 {
  require(
    requirement = eventTimestampMs.isDefined || startTimeMs.isDefined || endTimeMs.isDefined,
    message     = "At least one of eventTimestampMs, startTimeMs or endTimeMs must be defined."
  )
}

object AccountConcernEventDetails {
  val httpReads: Reads[AccountConcernEventDetails] = Json.reads[AccountConcernEventDetails]
}

case class AccountInterventionEventDetails(subjectId: String, credId: String, initiatingEntity: Option[InitiatingEntity], state: Option[String], action: Option[String], eventTimestampMs: Long)
    extends EventRequestDetailsV1

object AccountInterventionEventDetails {
  val httpReads: Reads[AccountInterventionEventDetails] = Json.reads[AccountInterventionEventDetails]
}

case class CredentialCompromiseEventDetails(initiatingEntity: Option[InitiatingEntity],
                                            credentialType: Option[CredentialType],
                                            eventTimestampMs: Long,
                                            reasonAdmin: Option[String],
                                            reasonUser: Option[String],
                                            subjectId: String,
                                            credId: String,
                                            emailAddress: Option[String],
                                            interventionCode: Option[InterventionCode]
                                           )
    extends EventRequestDetailsV1

object CredentialCompromiseEventDetails {
  val httpReads: Reads[CredentialCompromiseEventDetails] = Json.reads[CredentialCompromiseEventDetails]
}
