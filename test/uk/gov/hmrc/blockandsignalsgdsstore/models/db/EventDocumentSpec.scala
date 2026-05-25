/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.db

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.InitiatingEntity
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.{Event, EventData, EventDocument}

import java.time.{Clock, Instant}

class EventDocumentSpec extends AnyWordSpec with Matchers {

  "mongoFormat" should {
    "read json to EventDocument instance" in {
      val eventDocument = Json
        .obj(
          "event" -> Json.obj(
            "eventType"         -> "accountConcern",
            "originalEventType" -> "TICF_ACCOUNT_INTERVENTION",
            "eventId"           -> "756E69717565206964656E746966696572",
            "generatedAt"       -> 1730392175L,
            "subjectId"         -> "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
            "credId"            -> "3434343434343434",
            "eventData" -> Json.obj(
              "initiatingEntity" -> "analyst",
              "reason"           -> "account-takeover",
              "rationale"        -> "RA99",
              "eventTimestampMs" -> 1735714800000L,
              "startTimeMs"      -> 1735714800000L,
              "endTimeMs"        -> 1735722000000L
            )
          ),
          "storedAt" -> Json.obj(
            "$date" -> Json.obj(
              "$numberLong" -> "1736503810000"
            )
          ),
          "timeOfInterest" -> Json.obj(
            "$date" -> Json.obj(
              "$numberLong" -> "1735722000000"
            )
          )
        )
        .validate[EventDocument](EventDocument.mongoFormat)

      eventDocument shouldBe a[JsSuccess[EventDocument]]
      eventDocument.get shouldBe EventDocument(
        event = Event(
          EventType.AccountConcern,
          "TICF_ACCOUNT_INTERVENTION",
          "756E69717565206964656E746966696572",
          1730392175L,
          Some("urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw="),
          Some("3434343434343434"),
          AccountConcernEventData(
            Some(InitiatingEntity.Analyst),
            Some("account-takeover"),
            Some("RA99"),
            Some(1735714800000L),
            Some(1735714800000L),
            Some(1735722000000L)
          )
        ),
        storedAt       = Instant.ofEpochMilli(1736503810000L),
        timeOfInterest = Instant.ofEpochMilli(1735722000000L),
        action         = None
      )

      val anotherEventDocument = Json
        .obj(
          "event" -> Json.obj(
            "eventType"         -> "accountIntervention",
            "originalEventType" -> "AIS_EVENT_TRANSITION_APPLIED",
            "eventId"           -> "756E69717565206964656E746966696572",
            "generatedAt"       -> 1730392175L,
            "subjectId"         -> "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
            "credId"            -> "3434343434343434",
            "eventData" -> Json.obj(
              "initiatingEntity" -> "analyst",
              "state"            -> "active",
              "action"           -> "re-prove_identity",
              "eventTimestampMs" -> 1507644997001L
            )
          ),
          "storedAt" -> Json.obj(
            "$date" -> Json.obj(
              "$numberLong" -> "1736503810000"
            )
          ),
          "timeOfInterest" -> Json.obj(
            "$date" -> Json.obj(
              "$numberLong" -> "1507644997001"
            )
          ),
          "action" -> "re-prove_identity"
        )
        .validate[EventDocument](EventDocument.mongoFormat)
      anotherEventDocument shouldBe a[JsSuccess[EventDocument]]
      anotherEventDocument.get shouldBe EventDocument(
        Event(
          EventType.AccountIntervention,
          "AIS_EVENT_TRANSITION_APPLIED",
          "756E69717565206964656E746966696572",
          1730392175L,
          Some("urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw="),
          Some("3434343434343434"),
          AccountInterventionEventData(
            Some(InitiatingEntity.Analyst),
            Some("active"),
            Some("re-prove_identity"),
            1507644997001L
          )
        ),
        Instant.ofEpochMilli(1736503810000L),
        Instant.ofEpochMilli(1507644997001L),
        Some("re-prove_identity")
      )
    }

    "write EventDocument instance to json" in {
      val eventDocument = EventDocument(
        event = Event(
          EventType.AccountConcern,
          "TICF_ACCOUNT_INTERVENTION",
          "756E69717565206964656E746966696572",
          1730392175L,
          Some("urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw="),
          Some("3434343434343434"),
          AccountConcernEventData(
            Some(InitiatingEntity.Analyst),
            Some("account-takeover"),
            Some("RA99"),
            Some(1735714800000L),
            Some(1735714800000L),
            Some(1735722000000L)
          )
        ),
        storedAt       = Instant.ofEpochMilli(1736503810000L),
        timeOfInterest = Instant.ofEpochMilli(1735722000000L),
        action         = None
      )

      val json = Json.toJson(eventDocument)(EventDocument.mongoFormat)

      json shouldBe Json.obj(
        "event" -> Json.obj(
          "eventType"         -> "accountConcern",
          "originalEventType" -> "TICF_ACCOUNT_INTERVENTION",
          "eventId"           -> "756E69717565206964656E746966696572",
          "generatedAt"       -> 1730392175L,
          "subjectId"         -> "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
          "credId"            -> "3434343434343434",
          "eventData" -> Json.obj(
            "initiatingEntity" -> "analyst",
            "reason"           -> "account-takeover",
            "rationale"        -> "RA99",
            "eventTimestampMs" -> 1735714800000L,
            "startTimeMs"      -> 1735714800000L,
            "endTimeMs"        -> 1735722000000L
          )
        ),
        "storedAt" -> Json.obj(
          "$date" -> Json.obj(
            "$numberLong" -> "1736503810000"
          )
        ),
        "timeOfInterest" -> Json.obj(
          "$date" -> Json.obj(
            "$numberLong" -> "1735722000000"
          )
        )
      )

      val interventionEventDocument = EventDocument(
        Event(
          EventType.AccountIntervention,
          "AIS_EVENT_TRANSITION_APPLIED",
          "756E69717565206964656E746966696572",
          1730392175L,
          Some("urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw="),
          Some("3434343434343434"),
          AccountInterventionEventData(
            Some(InitiatingEntity.Analyst),
            Some("active"),
            Some("re-prove_identity"),
            1507644997001L
          )
        ),
        Instant.ofEpochMilli(1736503810000L),
        Instant.ofEpochMilli(1507644997001L),
        Some("re-prove_identity")
      )

      val interventionEventDocumentJson = Json.toJson(interventionEventDocument)(EventDocument.mongoFormat)

      interventionEventDocumentJson shouldBe Json.obj(
        "event" -> Json.obj(
          "eventType"         -> "accountIntervention",
          "originalEventType" -> "AIS_EVENT_TRANSITION_APPLIED",
          "eventId"           -> "756E69717565206964656E746966696572",
          "generatedAt"       -> 1730392175L,
          "subjectId"         -> "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
          "credId"            -> "3434343434343434",
          "eventData" -> Json.obj(
            "initiatingEntity" -> "analyst",
            "state"            -> "active",
            "action"           -> "re-prove_identity",
            "eventTimestampMs" -> 1507644997001L
          )
        ),
        "storedAt" -> Json.obj(
          "$date" -> Json.obj(
            "$numberLong" -> "1736503810000"
          )
        ),
        "timeOfInterest" -> Json.obj(
          "$date" -> Json.obj(
            "$numberLong" -> "1507644997001"
          )
        ),
        "action" -> "re-prove_identity"
      )
    }
  }

  "maskId()" should {
    "mask a credential ID" in {
      EventDocument.maskId(Some("1234567890123456")) shouldBe "************3456"
    }

    "mask a subject ID" in {
      val maskedId = EventDocument.maskId(Some("urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw="))
      maskedId shouldBe ("*" * 60) + "hYw="
    }

    "mask invalid IDs that are too short" in {
      EventDocument.maskId(None)              shouldBe "empty"
      EventDocument.maskId(Some(""))          shouldBe "empty"
      EventDocument.maskId(Some("abcd"))      shouldBe "****"
      EventDocument.maskId(Some("abcde"))     shouldBe "****e"
      EventDocument.maskId(Some("abcdefgh"))  shouldBe "****efgh"
      EventDocument.maskId(Some("abcdefghi")) shouldBe "*****fghi"
    }
  }

  "toLogString" should {
    "correctly serialise a Concern EventDocument with all fields present" in {
      val dateTimeString = "2021-01-02T15:16:17.999Z"
      val instant = Instant.parse(dateTimeString)

      val event = Event(
        eventType         = EventType.AccountConcern,
        originalEventType = "original-event-type",
        eventId           = "event-id",
        generatedAt       = 1234L,
        subjectId         = Some("urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw="),
        credId            = Some("1234567890123456"),
        eventData         = AccountConcernEventData(None, None, None, Some(instant.toEpochMilli), None, None)
      )
      val document = EventDocument(
        event          = event,
        storedAt       = instant,
        timeOfInterest = instant,
        action         = Some("some-action")
      )
      EventDocument.toLogString(document) shouldBe
        "credId: ************3456, " +
        "subId: " + ("*" * 60) + "hYw=" + ", " +
        "eventId: event-id, " +
        "action: some-action, " +
        "eventType: accountConcern, " +
        s"eventData: AccountConcernEventData(None,None,None,Some(${instant.toEpochMilli}),None,None), " +
        s"timeOfInterest: $dateTimeString"
    }

    "correctly serialise an Intervention EventDocument without an action and IDs" in {
      val eventTimestampMs = Instant.parse("2021-01-01T12:16:17.999Z").toEpochMilli
      val event = Event(
        eventType         = EventType.AccountIntervention,
        originalEventType = "original-event-type",
        eventId           = "event-id",
        generatedAt       = 1234L,
        subjectId         = Some(""),
        credId            = Some(""),
        eventData         = AccountInterventionEventData(None, None, None, eventTimestampMs)
      )
      val dateTimeString = "2021-01-02T15:16:17.999Z"
      val instant = Instant.parse(dateTimeString)
      val document = EventDocument(
        event          = event,
        storedAt       = instant,
        timeOfInterest = instant,
        action         = None
      )
      EventDocument.toLogString(document) shouldBe
        "credId: empty, " +
        "subId: empty, " +
        "eventId: event-id, " +
        "action: no-action, " +
        "eventType: accountIntervention, " +
        s"eventData: AccountInterventionEventData(None,None,None,$eventTimestampMs), " +
        s"timeOfInterest: $dateTimeString"
    }
  }

}
