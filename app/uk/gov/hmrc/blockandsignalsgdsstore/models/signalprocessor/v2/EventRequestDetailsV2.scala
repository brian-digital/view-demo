/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v2

import play.api.libs.json.*
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.*

sealed trait EventRequestDetailsV2

object EventRequestDetailsV2 {

  private val credentialConcernReadsWidened = CredentialConcernEventDetails.httpReads.widen[EventRequestDetailsV2]
  private val deviceConcernReadsWidened = DeviceConcernEventDetails.httpReads.widen[EventRequestDetailsV2]

  def httpReads(eventType: RequestEventTypeV2): Reads[EventRequestDetailsV2] = {
    eventType match
      case RequestEventTypeV2.CredentialConcern => credentialConcernReadsWidened
      case RequestEventTypeV2.DeviceConcern     => deviceConcernReadsWidened
  }
}

case class CredentialConcernEventDetails(accountIdentifierUri: String,
                                         credentialType: Option[CredentialType],
                                         credId: Option[String],
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
    extends EventRequestDetailsV2

object CredentialConcernEventDetails {
  val httpReads: Reads[CredentialConcernEventDetails] =
    Json.reads[CredentialConcernEventDetails].flatMapResult { details =>
      if details.eventTimestampMs.isEmpty && details.startTimeMs.isEmpty && details.endTimeMs.isEmpty
      then JsError("CredentialConcernEventDetails is missing all timestamps")
      else JsSuccess(details)
    }
}

case class DeviceConcernEventDetails(accountIdentifierUri: String,
                                     credId: Option[String],
                                     initiatingEntity: InitiatingEntity,
                                     reasonAdmin: Reason,
                                     rationale: Rationale,
                                     eventTimestampMs: Option[Long],
                                     startTimeMs: Option[Long],
                                     endTimeMs: Option[Long],
                                     iss: Option[String],
                                     sub: Option[String],
                                     identifiers: Option[List[DeviceConcernIdentifier]],
                                     sourceType: Option[SourceType],
                                     sourceTypeUri: Option[String]
                                    )
    extends EventRequestDetailsV2

object DeviceConcernEventDetails {
  val httpReads: Reads[DeviceConcernEventDetails] = Json.reads[DeviceConcernEventDetails].flatMapResult { details =>
    if details.eventTimestampMs.isEmpty && details.startTimeMs.isEmpty && details.endTimeMs.isEmpty
    then JsError("DeviceConcernEventDetails is missing all timestamps")
    else JsSuccess(details)
  }
}
