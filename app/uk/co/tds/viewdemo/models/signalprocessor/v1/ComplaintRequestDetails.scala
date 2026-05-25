/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.models.signalprocessor.v1

import play.api.libs.json.*
import uk.co.tds.viewdemo.models.common.{CredentialType, InitiatingEntity, InterventionCode}
import uk.co.tds.viewdemo.models.common.*

sealed trait ComplaintRequestDetails

object ComplaintRequestDetails {

  private val accountComplaintReadsWidened = AccountComplaintUserDetails.httpReads.widen[ComplaintRequestDetails]
  private val accessComplaintReadsWidened = AccessComplaintDetails.httpReads.widen[ComplaintRequestDetails]
  private val idCompromiseReadsWidened = IdCompromiseDetails.httpReads.widen[ComplaintRequestDetails]

  def httpReads(complaintType: RequestComplaintType): Reads[ComplaintRequestDetails] = {
    complaintType match
      case RequestComplaintType.AccountComplaint  => accountComplaintReadsWidened
      case RequestComplaintType.AccessComplaint   => accessComplaintReadsWidened
      case RequestComplaintType.IdCompromise      => idCompromiseReadsWidened
  }

  val mongoWrites: Writes[ComplaintRequestDetails] = Writes {
    case crd: AccountComplaintUserDetails => AccountComplaintUserDetails.mongoWrites.writes(crd)
    case crd: AccessComplaintDetails => AccessComplaintDetails.mongoWrites.writes(crd)
    case crd: IdCompromiseDetails => IdCompromiseDetails.mongoWrites.writes(crd)
  }
}

case class AccountComplaintUserDetails(subjectId: String,
                                       userId: String,
                                       initiatingEntity: Option[InitiatingEntity],
                                       reason: Option[String],
                                       rationale: Option[String],
                                       eventTimestampMs: Option[Long],
                                       startTimeMs: Option[Long],
                                       endTimeMs: Option[Long]
                                     )
    extends ComplaintRequestDetails {
  require(
    requirement = eventTimestampMs.isDefined || startTimeMs.isDefined || endTimeMs.isDefined,
    message     = "At least one of eventTimestampMs, startTimeMs or endTimeMs must be defined."
  )
}

object AccountComplaintUserDetails {
  val httpReads: Reads[AccountComplaintUserDetails] = Json.reads[AccountComplaintUserDetails]
  val mongoWrites: Writes[AccountComplaintUserDetails] = Json.writes[AccountComplaintUserDetails]
}

case class AccessComplaintDetails(subjectId: String, userId: String, initiatingEntity: Option[InitiatingEntity], state: Option[String], action: Option[String], eventTimestampMs: Long)
    extends ComplaintRequestDetails

object AccessComplaintDetails {
  val httpReads: Reads[AccessComplaintDetails] = Json.reads[AccessComplaintDetails]
  val mongoWrites: Writes[AccessComplaintDetails] = Json.writes[AccessComplaintDetails]
}

case class IdCompromiseDetails(initiatingEntity: Option[InitiatingEntity],
                               credentialType: Option[CredentialType],
                               eventTimestampMs: Long,
                               reasonAdmin: Option[String],
                               reasonUser: Option[String],
                               subjectId: String,
                               userId: String,
                               emailAddress: Option[String],
                               interventionCode: Option[InterventionCode]
                                           )
    extends ComplaintRequestDetails

object IdCompromiseDetails {
  val httpReads: Reads[IdCompromiseDetails] = Json.reads[IdCompromiseDetails]
  val mongoWrites: Writes[IdCompromiseDetails] = Json.writes[IdCompromiseDetails]
}
