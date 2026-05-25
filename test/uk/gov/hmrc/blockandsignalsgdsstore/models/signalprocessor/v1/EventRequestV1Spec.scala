/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v1

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.JsValue
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.InterventionCode.PasswordReset
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.{CredentialType, InitiatingEntity}
import uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.*
import uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v1.*
import uk.gov.hmrc.blockandsignalsgdsstore.utils.TestData

class EventRequestV1Spec extends AnyWordSpec with Matchers with TestData {

  private def makeEventRequest(eventType: RequestEventTypeV1, includeOptionalFields: Boolean): EventRequestV1 = {

    def includeOptional[T](value: T): Option[T] = {
      if includeOptionalFields then Some(value) else None
    }

    val metadata = EventRequestMetadataV1(
      signalsEventType  = eventType,
      originalEventType = "ORIGINAL_EVENT_TYPE",
      jti               = "756E69717565206964656E746966696572",
      iat               = 1730392175
    )

    val details = eventType match {
      case RequestEventTypeV1.AccountConcern =>
        AccountConcernEventDetails(
          subjectId        = "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
          credId           = "3434343434343434",
          initiatingEntity = includeOptional(InitiatingEntity.Analyst),
          reason           = includeOptional("account-takeover"),
          rationale        = includeOptional("RA99"),
          eventTimestampMs = Some(1507644997001L), // at least one of timestampMs must be present
          startTimeMs      = includeOptional(1507644997001L),
          endTimeMs        = includeOptional(1507644997001L)
        )
      case RequestEventTypeV1.AccountIntervention =>
        AccountInterventionEventDetails(
          subjectId        = "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
          credId           = "3434343434343434",
          initiatingEntity = includeOptional(InitiatingEntity.Analyst),
          state            = includeOptional("active"),
          action           = includeOptional("re-prove_identity"),
          eventTimestampMs = 1507644997001L
        )
      case RequestEventTypeV1.CredentialCompromise =>
        CredentialCompromiseEventDetails(
          initiatingEntity = includeOptional(InitiatingEntity.Analyst),
          credentialType   = includeOptional(CredentialType.Email),
          eventTimestampMs = 1507644997001L,
          reasonAdmin      = includeOptional("mfa email mismatch"),
          reasonUser       = includeOptional("mfa email mismatch"),
          subjectId        = "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
          credId           = "3434343434343434",
          emailAddress     = includeOptional("test@example.com"),
          interventionCode = includeOptional(PasswordReset)
        )
    }

    EventRequestV1(metadata, details)
  }

  private def test(input: JsValue, expectedResult: Either[String, EventRequestV1]): Assertion = {
    val jsResult = input.validate[EventRequestV1](EventRequestV1.httpReads)
    expectedResult match
      case Left(expectedError) =>
        jsResult.isError shouldBe true
        val error = jsResult.asEither.swap.map(_.head._2.head.message).getOrElse("")
        error shouldBe expectedError
      case Right(expectedEventRequest) => jsResult.get shouldBe expectedEventRequest
  }

  "EventRequestV1 httpReads" should {
    "read from valid json" when {
      "the event type is account-concern and optional fields are present" in {
        test(
          input          = EventRequestV1Json.accountConcernJsonFull,
          expectedResult = Right(makeEventRequest(RequestEventTypeV1.AccountConcern, true))
        )
      }
      "the event type is account-concern and optional fields are absent" in {
        test(
          input          = EventRequestV1Json.accountConcernJsonNoOptionals,
          expectedResult = Right(makeEventRequest(RequestEventTypeV1.AccountConcern, false))
        )
      }

      "the event type is account-intervention and optional fields are present" in {
        test(
          input          = EventRequestV1Json.accountInterventionJsonFull,
          expectedResult = Right(makeEventRequest(RequestEventTypeV1.AccountIntervention, true))
        )
      }
      "the event type is account-intervention and optional fields are absent" in {
        test(
          input          = EventRequestV1Json.accountInterventionJsonNoOptionals,
          expectedResult = Right(makeEventRequest(RequestEventTypeV1.AccountIntervention, false))
        )
      }

      "the event type is credential-compromise and optional fields are present" in {
        test(
          input          = EventRequestV1Json.credentialCompromiseJsonFull,
          expectedResult = Right(makeEventRequest(RequestEventTypeV1.CredentialCompromise, true))
        )
      }
      "the event type is credential-compromise and optional fields are absent" in {
        test(
          input          = EventRequestV1Json.credentialCompromiseJsonNoOptionals,
          expectedResult = Right(makeEventRequest(RequestEventTypeV1.CredentialCompromise, false))
        )
      }
    }

    "reject invalid json" when {
      "signalsEventType is not valid" in {
        test(
          input          = EventRequestV1Json.unknownEventJson,
          expectedResult = Left("Unknown RequestEventTypeV1: unknownEvent")
        )
      }
      "the event type is account-concern and all timestamp fields are absent" in {
        intercept[IllegalArgumentException](
          EventRequestV1Json.accountConcernInvalidJsonNoTimestamp.validate[EventRequestV1](EventRequestV1.httpReads)
        ).getMessage shouldBe "requirement failed: At least one of eventTimestampMs, startTimeMs or endTimeMs must be defined."
      }
    }
  }
}
