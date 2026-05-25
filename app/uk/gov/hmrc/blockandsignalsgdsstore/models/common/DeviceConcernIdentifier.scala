/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.common

import play.api.libs.json.*

case class DeviceConcernIdentifier(format: DeviceConcernIdentifierType, value: String)

object DeviceConcernIdentifier {
  given reads: Reads[DeviceConcernIdentifier] = Json.reads[DeviceConcernIdentifier]
  given writes: Writes[DeviceConcernIdentifier] = Json.writes[DeviceConcernIdentifier]
}

enum DeviceConcernIdentifierType(val stringValue: String):
  case DeviceHash          extends DeviceConcernIdentifierType("device-hash")
  case DeviceId            extends DeviceConcernIdentifierType("device-id")
  case JourneyId           extends DeviceConcernIdentifierType("journey-id")
  case PersistentSessionId extends DeviceConcernIdentifierType("persistent-session-id")
  case SessionId           extends DeviceConcernIdentifierType("session-id")
  case UserDeviceIpAddress extends DeviceConcernIdentifierType("user-device-ip-address")

object DeviceConcernIdentifierType {
  given reads: Reads[DeviceConcernIdentifierType] = Reads {
    case JsString(s) =>
      values.find(_.stringValue == s).map(JsSuccess(_)).getOrElse(JsError(s"Unknown DeviceConcernIdentifierType: $s"))
    case _ => JsError("Expected string for DeviceConcernIdentifierType")
  }

  given writes: Writes[DeviceConcernIdentifierType] = Writes(ie => JsString(ie.stringValue))
}
