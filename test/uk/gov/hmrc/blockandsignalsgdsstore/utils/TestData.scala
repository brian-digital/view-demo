/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.utils

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.*
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.InterventionCode.PasswordReset
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.*
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.SearchResultV2
import uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v1.{AccountConcernEventDetails, AccountInterventionEventDetails, CredentialCompromiseEventDetails, EventRequestMetadataV1, EventRequestV1, RequestEventTypeV1}
import uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v2.*

import java.time.Instant

trait TestData {
  object EventRequestV1Json {
    object Strings {
      val accountConcernJsonFull: String =
        s"""
           |{
           |  "metadata": {
           |    "signalsEventType": "accountConcern",
           |    "originalEventType": "ORIGINAL_EVENT_TYPE",
           |    "jti" : "756E69717565206964656E746966696572",
           |    "iat" : 1730392175
           |  },
           |  "details": {
           |    "subjectId": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
           |    "credId": "3434343434343434",
           |    "initiatingEntity": "analyst",
           |    "reason": "account-takeover",
           |    "rationale": "RA99",
           |    "eventTimestampMs": 1507644997001,
           |    "startTimeMs": 1507644997001,
           |    "endTimeMs": 1507644997001
           |  }
           |}
           |""".stripMargin

      val accountConcernJsonNoOptionals: String =
        s"""
           |{
           |  "metadata": {
           |    "signalsEventType": "accountConcern",
           |    "originalEventType": "ORIGINAL_EVENT_TYPE",
           |    "jti": "756E69717565206964656E746966696572",
           |    "iat": 1730392175
           |  },
           |  "details": {
           |    "subjectId": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
           |    "credId": "3434343434343434",
           |    "eventTimestampMs": 1507644997001
           |  }
           |}
           |""".stripMargin

      val accountConcernInvalidJsonNoTimestamp: String =
        s"""
           |{
           |  "metadata": {
           |    "signalsEventType": "accountConcern",
           |    "originalEventType": "ORIGINAL_EVENT_TYPE",
           |    "jti": "756E69717565206964656E746966696572",
           |    "iat": 1730392175
           |  },
           |  "details": {
           |    "subjectId": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
           |    "credId": "3434343434343434"
           |  }
           |}
           |""".stripMargin

      val accountInterventionJsonFull: String = accountInterventionJsonFullWithJti("756E69717565206964656E746966696572")
      def accountInterventionJsonFullWithJti(jti: String): String =
        s"""
           |{
           |  "metadata": {
           |    "signalsEventType": "accountIntervention",
           |    "originalEventType": "ORIGINAL_EVENT_TYPE",
           |    "jti" : "$jti",
           |    "iat" : 1730392175
           |  },
           |  "details": {
           |    "subjectId": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
           |    "credId": "3434343434343434",
           |    "initiatingEntity": "analyst",
           |    "state":  "active",
           |    "action" : "re-prove_identity",
           |    "eventTimestampMs": 1507644997001
           |  }
           |}
           |""".stripMargin

      val accountInterventionJsonNoOptionals: String =
        s"""
           |{
           |  "metadata": {
           |    "signalsEventType": "accountIntervention",
           |    "originalEventType": "ORIGINAL_EVENT_TYPE",
           |    "jti" : "756E69717565206964656E746966696572",
           |    "iat" : 1730392175
           |  },
           |  "details": {
           |    "subjectId": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
           |    "credId": "3434343434343434",
           |    "eventTimestampMs": 1507644997001
           |  }
           |}
           |""".stripMargin

      val credentialCompromiseJsonFull: String =
        s"""
           |{
           |  "metadata": {
           |    "signalsEventType": "credential-compromise",
           |    "originalEventType": "ORIGINAL_EVENT_TYPE",
           |    "jti" : "756E69717565206964656E746966696572",
           |    "iat" : 1730392175
           |  },
           |  "details": {
           |    "initiatingEntity": "analyst",
           |    "subjectId": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
           |    "credId": "3434343434343434",
           |    "credentialType": "email",
           |    "eventTimestampMs": 1507644997001,
           |    "reasonAdmin": "mfa email mismatch",
           |    "reasonUser": "mfa email mismatch",
           |    "emailAddress": "test@example.com",
           |    "interventionCode": "04"
           |  }
           |}
           |""".stripMargin

      val credentialCompromiseWrongInterventionCode: String =
        s"""
           |{
           |  "metadata": {
           |    "signalsEventType": "credential-compromise",
           |    "originalEventType": "ORIGINAL_EVENT_TYPE",
           |    "jti" : "756E69717565206964656E746966696572",
           |    "iat" : 1730392175
           |  },
           |  "details": {
           |    "initiatingEntity": "analyst",
           |    "subjectId": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
           |    "credId": "3434343434343434",
           |    "credentialType": "email",
           |    "eventTimestampMs": 1507644997001,
           |    "reasonAdmin": "mfa email mismatch",
           |    "reasonUser": "mfa email mismatch",
           |    "emailAddress": "test@example.com",
           |    "interventionCode": "09"
           |  }
           |}
           |""".stripMargin

      val credentialCompromiseJsonNoOptionals: String =
        s"""
           |{
           |  "metadata": {
           |    "signalsEventType": "credential-compromise",
           |    "originalEventType": "ORIGINAL_EVENT_TYPE",
           |    "jti" : "756E69717565206964656E746966696572",
           |    "iat" : 1730392175
           |  },
           |  "details": {
           |    "subjectId": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
           |    "credId": "3434343434343434",
           |    "eventTimestampMs": 1507644997001
           |  }
           |}
           |""".stripMargin

      val unknownEventJson: String =
        s"""
           |{
           |  "metadata": {
           |    "signalsEventType": "unknownEvent",
           |    "originalEventType": "ORIGINAL_EVENT_TYPE",
           |    "jti" : "756E69717565206964656E746966696572",
           |    "iat" : 1730392175
           |  },
           |  "details": {
           |    "subjectId": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
           |    "credId": "3434343434343434"
           |  }
           |}
           |""".stripMargin

      val invalidDetailsJson: String =
        s"""
           |{
           |  "metadata": {
           |    "signalsEventType": "credential-compromise",
           |    "originalEventType": "ORIGINAL_EVENT_TYPE",
           |    "jti" : "756E69717565206964656E746966696572",
           |    "iat" : 1730392175
           |  },
           |  "details": {
           |  }
           |}
           |""".stripMargin
    }

    val accountConcernJsonFull: JsValue = Json.parse(Strings.accountConcernJsonFull)
    val accountConcernJsonNoOptionals: JsValue = Json.parse(Strings.accountConcernJsonNoOptionals)
    val accountConcernInvalidJsonNoTimestamp: JsValue = Json.parse(Strings.accountConcernInvalidJsonNoTimestamp)

    val accountInterventionJsonFull: JsValue = Json.parse(Strings.accountInterventionJsonFull)
    val accountInterventionJsonNoOptionals: JsValue = Json.parse(Strings.accountInterventionJsonNoOptionals)
    def accountInterventionJsonFullWithJti(jti: String): JsValue =
      Json.parse(Strings.accountInterventionJsonFullWithJti(jti))

    val credentialCompromiseJsonFull: JsValue = Json.parse(Strings.credentialCompromiseJsonFull)
    val credentialCompromiseJsonNoOptionals: JsValue = Json.parse(Strings.credentialCompromiseJsonNoOptionals)
    val credentialCompromiseWrongInterventionCodeJson: JsValue =
      Json.parse(Strings.credentialCompromiseWrongInterventionCode)

    val unknownEventJson: JsValue = Json.parse(Strings.unknownEventJson)
    val invalidDetailsJson: JsValue = Json.parse(Strings.invalidDetailsJson)
  }

  object EventRequestV1Models {
    val accountConcernRequestModelFull = EventRequestV1(
      metadata = EventRequestMetadataV1(
        signalsEventType  = RequestEventTypeV1.AccountConcern,
        originalEventType = "ORIGINAL_EVENT_TYPE",
        jti               = "756E69717565206964656E746966696572",
        iat               = 1730392175L
      ),
      details = AccountConcernEventDetails(
        subjectId        = "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
        credId           = "3434343434343434",
        initiatingEntity = Some(InitiatingEntity.Analyst),
        reason           = Some("account-takeover"), // TODO [WD]
        rationale        = Some("RA99"), // TODO [WD]
        eventTimestampMs = Some(1507644997001L),
        startTimeMs      = Some(1507644997001L),
        endTimeMs        = Some(1507644997001L)
      )
    )

    val accountInterventionRequestModelFull = EventRequestV1(
      metadata = EventRequestMetadataV1(
        signalsEventType  = RequestEventTypeV1.AccountIntervention,
        originalEventType = "ORIGINAL_EVENT_TYPE",
        jti               = "756E69717565206964656E746966696572",
        iat               = 1730392175L
      ),
      details = AccountInterventionEventDetails(
        subjectId        = "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
        credId           = "3434343434343434",
        initiatingEntity = Some(InitiatingEntity.Analyst),
        state            = Some("active"),
        action           = Some("re-prove_identity"),
        eventTimestampMs = 1507644997001L
      )
    )

    val credentialCompromiseRequestModelFull = EventRequestV1(
      metadata = EventRequestMetadataV1(
        signalsEventType  = RequestEventTypeV1.CredentialCompromise,
        originalEventType = "ORIGINAL_EVENT_TYPE",
        jti               = "756E69717565206964656E746966696572",
        iat               = 1730392175L
      ),
      details = CredentialCompromiseEventDetails(
        initiatingEntity = Some(InitiatingEntity.Analyst),
        credentialType   = Some(CredentialType.Email),
        eventTimestampMs = 1507644997001L,
        reasonAdmin      = Some("mfa email mismatch"),
        reasonUser       = Some("mfa email mismatch"),
        subjectId        = "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
        credId           = "3434343434343434",
        emailAddress     = Some("test@example.com"),
        interventionCode = Some(InterventionCode.PasswordReset)
      )
    )
  }

  object EventRequestV2Json {

    object Strings {

      // TODO [WD] This is not full
      val credentialConcernJsonFull: String =
        s"""
           |{
           |  "metadata": {
           |    "signalsEventType": "credentialConcern",
           |    "originalEventType": "ORIGINAL_EVENT_TYPE",
           |    "jti": "756E69717565206964656E746966696572",
           |    "iat": 1730392175
           |  },
           |  "details": {
           |    "eventTimestampMs": 1507644997001,
           |    "credId": "3434343434343434",
           |    "sourceType": "darkweb-market",
           |    "icaoIssuerCode": "GBR",
           |    "documentNumber": "ZU30861133",
           |    "initiatingEntity": "analyst",
           |    "reasonAdmin": "credential-compromise",
           |    "rationale": "RA99",
           |    "credentialType": "IR",
           |    "identifierFormat": "document-identifier",
           |    "accountIdentifierUri": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
           |    "sourceTypeUri": "https://darkweb-site.xyz/abc"
           |  }
           |}
           |""".stripMargin

      // TODO [WD] This is not full
      val deviceConcernJsonFull: String =
        s"""
           |{
           |  "metadata": {
           |    "signalsEventType": "deviceConcern",
           |    "originalEventType": "ORIGINAL_EVENT_TYPE",
           |    "jti": "756E69717565206964656E746966696572",
           |    "iat": 1730392175
           |  },
           |  "details": {
           |    "eventTimestampMs": 1507644997001,
           |    "accountIdentifierUri": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
           |    "credId": "3434343434343434",
           |    "initiatingEntity": "analyst",
           |    "reasonAdmin": "synthetic-identity",
           |    "rationale": "RA99",
           |    "identifiers": [
           |      {
           |        "format": "device-hash",
           |        "value": "MpCu20hrMHE-NwguDmQVO44YJv2"
           |      }
           |    ]
           |  }
           |}
           |""".stripMargin
    }

    val credentialConcernJsonFull: JsValue = Json.parse(Strings.credentialConcernJsonFull)

    val deviceConcernJsonFull: JsValue = Json.parse(Strings.deviceConcernJsonFull)
  }

  object EventRequestV2Models {
    val credentialConcernRequestModel = EventRequestV2(
      metadata = EventRequestMetadataV2(
        signalsEventType  = RequestEventTypeV2.CredentialConcern,
        originalEventType = "ORIGINAL_EVENT_TYPE",
        jti               = "756E69717565206964656E746966696572",
        iat               = 1730392175
      ),
      details = CredentialConcernEventDetails(
        accountIdentifierUri = "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
        credentialType       = Some(CredentialType.IR),
        credId               = Some("3434343434343434"),
        initiatingEntity     = InitiatingEntity.Analyst,
        reasonAdmin          = Reason.CredentialCompromise,
        rationale            = Rationale.RA99UnknownRationale,
        eventTimestampMs     = Some(1507644997001L),
        startTimeMs          = None,
        endTimeMs            = None,
        identifierFormat     = "document-identifier",
        documentNumber       = Some("ZU30861133"),
        expiryDate           = None,
        icaoIssuerCode       = Some("GBR"),
        personalNumber       = None,
        issueNumber          = None,
        issuedBy             = None,
        email                = None,
        phoneNumber          = None,
        nino                 = None,
        sourceType           = Some(SourceType.DarkWebMarket),
        sourceTypeUri        = Some("https://darkweb-site.xyz/abc")
      )
    )

    val deviceConcernRequestModel: EventRequestV2 = EventRequestV2(
      metadata = EventRequestMetadataV2(
        signalsEventType  = RequestEventTypeV2.DeviceConcern,
        originalEventType = "ORIGINAL_EVENT_TYPE",
        jti               = "756E69717565206964656E746966696572",
        iat               = 1730392175
      ),
      details = DeviceConcernEventDetails(
        accountIdentifierUri = "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
        credId               = Some("3434343434343434"),
        initiatingEntity     = InitiatingEntity.Analyst,
        reasonAdmin          = Reason.SyntheticIdentity,
        rationale            = Rationale.RA99UnknownRationale,
        eventTimestampMs     = Some(1507644997001L),
        startTimeMs          = None,
        endTimeMs            = None,
        iss                  = None,
        sub                  = None,
        identifiers = Some(
          List(
            DeviceConcernIdentifier(DeviceConcernIdentifierType.DeviceHash, "MpCu20hrMHE-NwguDmQVO44YJv2")
          )
        ),
        sourceType    = None,
        sourceTypeUri = None
      )
    )
  }

  object DatabaseModels {

    object JsonData {

      object Strings {
        val accountConcernEventDataJsonFull: String =
          s"""
             |{
             |  "initiatingEntity": "analyst",
             |  "reason": "account-takeover",
             |  "rationale": "RA99",
             |  "eventTimestampMs": 1507644997001,
             |  "startTimeMs": 1507644997001,
             |  "endTimeMs": 1507644997001
             |}
             |""".stripMargin
        val accountConcernEventJsonFull: String =
          makeEventJson(EventType.AccountConcern, accountConcernEventDataJsonFull)

        val accountInterventionEventDataJsonFull: String =
          s"""
             |{
             |  "initiatingEntity": "analyst",
             |  "state": "active",
             |  "action": "re-prove_identity",
             |  "eventTimestampMs": 1507644997001
             |}
             |""".stripMargin
        val accountInterventionEventJsonFull: String =
          makeEventJson(EventType.AccountIntervention, accountInterventionEventDataJsonFull)

        val credentialCompromiseEventDataJsonFull: String =
          s"""
             |{
             |  "initiatingEntity": "analyst",
             |  "credentialType": "email",
             |  "eventTimestampMs": 1507644997001,
             |  "reasonAdmin": "mfa email mismatch",
             |  "reasonUser": "mfa email mismatch",
             |  "emailAddress": "test@example.com",
             |  "interventionCode": "04"
             |}
             |""".stripMargin
        val credentialCompromiseEventJsonFull: String =
          makeEventJson(EventType.CredentialCompromise, credentialCompromiseEventDataJsonFull)

        def makeEventJson(eventType: EventType, eventDetailsJson: String): String = {
          s"""
             |{
             |  "eventType": "${eventType.mongoValue}",
             |  "originalEventType": "ORIGINAL_EVENT_TYPE",
             |  "eventId": "756E69717565206964656E746966696572",
             |  "generatedAt": 1730392175,
             |  "subjectId": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
             |  "credId": "3434343434343434",
             |  "eventData": $eventDetailsJson
             |}
             |
             |""".stripMargin
        }
      }

      val accountConcernEventDataJsonFull: JsValue = Json.parse(Strings.accountConcernEventDataJsonFull)
      val accountConcernEventJsonFull: JsValue = Json.parse(Strings.accountConcernEventJsonFull)

      val accountInterventionEventDataJsonFull: JsValue = Json.parse(Strings.accountInterventionEventDataJsonFull)
      val accountInterventionEventJsonFull: JsValue = Json.parse(Strings.accountInterventionEventJsonFull)

      val credentialCompromiseEventDataJsonFull: JsValue = Json.parse(Strings.credentialCompromiseEventDataJsonFull)
      val credentialCompromiseEventJsonFull: JsValue = Json.parse(Strings.credentialCompromiseEventJsonFull)
    }

    private def makeEvent(eventType: EventType, eventData: EventData): Event = Event(
      eventType         = eventType,
      originalEventType = "ORIGINAL_EVENT_TYPE",
      eventId           = "756E69717565206964656E746966696572",
      generatedAt       = 1730392175,
      subjectId         = Some("urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw="),
      credId            = Some("3434343434343434"),
      eventData         = eventData
    )

    val accountConcernEventDataFull: EventData = AccountConcernEventData(
      initiatingEntity = Some(InitiatingEntity.Analyst),
      reason           = Some("account-takeover"),
      rationale        = Some("RA99"),
      eventTimestampMs = Some(1507644997001L),
      startTimeMs      = Some(1507644997001L),
      endTimeMs        = Some(1507644997001L)
    )
    val accountConcernEventFull: Event = makeEvent(EventType.AccountConcern, accountConcernEventDataFull)

    val accountInterventionEventDataFull: EventData = AccountInterventionEventData(
      initiatingEntity = Some(InitiatingEntity.Analyst),
      state            = Some("active"),
      action           = Some("re-prove_identity"),
      eventTimestampMs = 1507644997001L
    )
    val accountInterventionEventFull: Event = makeEvent(EventType.AccountIntervention, accountInterventionEventDataFull)

    val credentialCompromiseEventDataFull: EventData = CredentialCompromiseEventData(
      initiatingEntity = Some(InitiatingEntity.Analyst),
      credentialType   = Some(CredentialType.Email),
      eventTimestampMs = 1507644997001L,
      reasonAdmin      = Some("mfa email mismatch"),
      reasonUser       = Some("mfa email mismatch"),
      emailAddress     = Some("test@example.com"),
      interventionCode = Some(PasswordReset)
    )
    val credentialCompromiseEventFull: Event = makeEvent(
      EventType.CredentialCompromise,
      credentialCompromiseEventDataFull
    )

    val credentialConcernEventDataFull: EventData = CredentialConcernEventData(
      credentialType   = Some(CredentialType.IR),
      initiatingEntity = InitiatingEntity.Analyst,
      reasonAdmin      = Reason.CredentialCompromise,
      rationale        = Rationale.RA99UnknownRationale,
      eventTimestampMs = Some(1507644997001L),
      startTimeMs      = None,
      endTimeMs        = None,
      identifierFormat = "document-identifier",
      documentNumber   = Some("ZU30861133"),
      expiryDate       = None,
      icaoIssuerCode   = Some("GBR"),
      personalNumber   = None,
      issueNumber      = None,
      issuedBy         = None,
      email            = None,
      phoneNumber      = None,
      nino             = None,
      sourceType       = Some(SourceType.DarkWebMarket),
      sourceTypeUri    = Some("https://darkweb-site.xyz/abc")
    )
    val credentialConcernEventFull: Event = makeEvent(EventType.CredentialConcern, credentialConcernEventDataFull)

    val deviceConcernEventDataFull: EventData = DeviceConcernEventData(
      initiatingEntity = InitiatingEntity.Analyst,
      reasonAdmin      = Reason.SyntheticIdentity,
      rationale        = Rationale.RA99UnknownRationale,
      eventTimestampMs = Some(1507644997001L),
      startTimeMs      = None,
      endTimeMs        = None,
      iss              = None,
      sub              = None,
      identifiers      = List(DeviceConcernIdentifier(DeviceConcernIdentifierType.DeviceHash, "MpCu20hrMHE-NwguDmQVO44YJv2")),
      sourceType       = None,
      sourceTypeUri    = None
    )
    val deviceConcernEventFull: Event = makeEvent(EventType.DeviceConcern, deviceConcernEventDataFull)
  }

  object SearchResultV2Models {

    def searchResultV2Helper(
      eventType: EventType,
      hmrcCredentialId: Option[String] = None,
      oneLoginSubjectId: Option[String] = None,
      submittedOn: Instant,
      submittedBy: Option[InitiatingEntity] = None,
      reason: Option[String] = None,
      rationale: Option[String] = None,
      accountInterventionState: Option[String] = None,
      accountInterventionAction: Option[String] = None,
      credentialCompromiseEmailAddress: Option[String] = None,
      credentialCompromiseInterventionCode: Option[String] = None,
      credentialConcernSourceType: Option[String] = None,
      credentialConcernSourceUri: Option[String] = None,
      credentialConcernCredentialType: Option[String] = None,
      credentialConcernIdentifierFormat: Option[String] = None,
      credentialConcernDocumentNumber: Option[String] = None,
      credentialConcernExpiryDate: Option[String] = None,
      credentialConcernIcaoIssuerCode: Option[String] = None,
      credentialConcernPersonalNumber: Option[String] = None,
      credentialConcernIssueNumber: Option[String] = None,
      credentialConcernIssuedBy: Option[String] = None,
      credentialConcernIssuingCountry: Option[String] = None,
      credentialConcernEmailAddress: Option[String] = None,
      credentialConcernTelephoneNumber: Option[String] = None,
      credentialConcernNino: Option[String] = None,
      deviceConcernSourceType: Option[String] = None,
      deviceConcernSourceUri: Option[String] = None,
      deviceConcernDeviceHash: Option[String] = None,
      deviceCookieJourneyId: Option[String] = None,
      deviceConcernPersistentSessionCookie: Option[String] = None,
      deviceConcernDeviceId: Option[String] = None,
      deviceConcernSessionId: Option[String] = None,
      deviceConcernIpAddress: Option[String] = None
    ): SearchResultV2 = SearchResultV2(
      eventType,
      hmrcCredentialId,
      oneLoginSubjectId,
      submittedOn,
      submittedBy,
      reason,
      rationale,
      accountInterventionState,
      accountInterventionAction,
      credentialCompromiseEmailAddress,
      credentialCompromiseInterventionCode,
      credentialConcernSourceType,
      credentialConcernSourceUri,
      credentialConcernCredentialType,
      credentialConcernIdentifierFormat,
      credentialConcernDocumentNumber,
      credentialConcernExpiryDate,
      credentialConcernIcaoIssuerCode,
      credentialConcernPersonalNumber,
      credentialConcernIssueNumber,
      credentialConcernIssuedBy,
      credentialConcernIssuingCountry,
      credentialConcernEmailAddress,
      credentialConcernTelephoneNumber,
      credentialConcernNino,
      deviceConcernSourceType,
      deviceConcernSourceUri,
      deviceConcernDeviceHash,
      deviceCookieJourneyId,
      deviceConcernPersistentSessionCookie,
      deviceConcernDeviceId,
      deviceConcernSessionId,
      deviceConcernIpAddress
    )

    val accountConcernFull: SearchResultV2 = searchResultV2Helper(
      eventType         = EventType.AccountConcern,
      hmrcCredentialId  = Some("3434343434343434"),
      oneLoginSubjectId = Some("urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw="),
      submittedOn       = Instant.ofEpochMilli(1507644997001L),
      submittedBy       = Some(InitiatingEntity.Analyst),
      reason            = Some("account-takeover"),
      rationale         = Some("RA99")
    )

    val accountInterventionFull: SearchResultV2 = searchResultV2Helper(
      eventType                 = EventType.AccountIntervention,
      hmrcCredentialId          = Some("3434343434343434"),
      oneLoginSubjectId         = Some("urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw="),
      submittedOn               = Instant.ofEpochMilli(1507644997001L),
      submittedBy               = Some(InitiatingEntity.Analyst),
      accountInterventionState  = Some("active"),
      accountInterventionAction = Some("re-prove_identity")
    )

    val credentialCompromiseFull: SearchResultV2 = searchResultV2Helper(
      eventType                            = EventType.CredentialCompromise,
      hmrcCredentialId                     = Some("3434343434343434"),
      oneLoginSubjectId                    = Some("urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw="),
      submittedOn                          = Instant.ofEpochMilli(1507644997001L),
      submittedBy                          = Some(InitiatingEntity.Analyst),
      reason                               = Some("mfa email mismatch"),
      credentialCompromiseEmailAddress     = Some("test@example.com"),
      credentialCompromiseInterventionCode = Some("04")
    )

    val credentialConcernFull: SearchResultV2 = searchResultV2Helper(
      eventType                         = EventType.CredentialConcern,
      hmrcCredentialId                  = Some("3434343434343434"),
      oneLoginSubjectId                 = Some("urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw="),
      submittedOn                       = Instant.ofEpochMilli(1507644997001L),
      submittedBy                       = Some(InitiatingEntity.Analyst),
      reason                            = Some(Reason.CredentialCompromise.stringValue),
      rationale                         = Some(Rationale.RA99UnknownRationale.code),
      credentialConcernSourceType       = Some("darkweb-market"),
      credentialConcernSourceUri        = Some("https://darkweb-site.xyz/abc"),
      credentialConcernCredentialType   = Some("IR"),
      credentialConcernIdentifierFormat = Some("document-identifier"),
      credentialConcernDocumentNumber   = Some("ZU30861133"),
      credentialConcernIcaoIssuerCode   = Some("GBR")
    )

    val deviceConcernFull: SearchResultV2 = searchResultV2Helper(
      eventType               = EventType.DeviceConcern,
      hmrcCredentialId        = Some("3434343434343434"),
      oneLoginSubjectId       = Some("urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw="),
      submittedOn             = Instant.ofEpochMilli(1507644997001L),
      submittedBy             = Some(InitiatingEntity.Analyst),
      reason                  = Some(Reason.SyntheticIdentity.stringValue),
      rationale               = Some(Rationale.RA99UnknownRationale.code),
      deviceConcernDeviceHash = Some("MpCu20hrMHE-NwguDmQVO44YJv2")
    )
  }
}
