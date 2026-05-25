/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.db

import play.api.libs.json.*

/** Represents the 'signals event type'
  * @param searchValue
  *   The string format used by block-and-signals-frontend
  * @param mongoValue
  *   The string format used to save events to block-and-signals-gds-store mongo collection
  */
enum EventType(val searchValue: String, val mongoValue: String) {
  case AccountIntervention  extends EventType("account-intervention", "accountIntervention")
  case AccountConcern       extends EventType("account-concern", "accountConcern")
  case CredentialCompromise extends EventType("credential-compromise", "credentialCompromise")
  case CredentialConcern    extends EventType("credential-concern", "credentialConcern")
  case DeviceConcern        extends EventType("device-concern", "deviceConcern")
}

object EventType {

  private def createReads(selector: EventType => String)(str: String): JsResult[EventType] =
    EventType.values
      .find(selector(_) == str)
      .map(JsSuccess(_))
      .getOrElse(JsError(s"Invalid eventType: $str"))

  val mongoReads: Reads[EventType] =
    Reads.of[String] flatMapResult createReads(_.mongoValue)

  val mongoWrites: Writes[EventType] =
    Writes(eventType => JsString(eventType.mongoValue))

  val mongoFormat: Format[EventType] = Format(
    mongoReads,
    mongoWrites
  )

  val searchValueReads: Reads[EventType] =
    Reads.of[String] flatMapResult createReads(_.searchValue)

  val searchValueWrites: Writes[EventType] =
    Writes(eventType => JsString(eventType.searchValue))
}
