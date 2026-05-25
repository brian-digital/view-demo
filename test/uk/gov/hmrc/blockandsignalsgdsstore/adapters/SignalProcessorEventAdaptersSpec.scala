/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.adapters

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.{AccountConcernEventData, AccountInterventionEventData, EventDocument}
import uk.gov.hmrc.blockandsignalsgdsstore.utils.TestData

import java.time.{Clock, Instant, ZoneId}

class SignalProcessorEventAdaptersSpec extends AnyWordSpec with Matchers with TestData {

  "V1 eventRequestToDbEvent()" should {
    "transform an account concern request" in {
      val input = EventRequestV1Models.accountConcernRequestModelFull
      val expected = DatabaseModels.accountConcernEventFull
      SignalProcessorEventAdapters.V1.eventRequestToDbEvent(input) shouldBe expected
    }

    "transform an account intervention request" in {
      val input = EventRequestV1Models.accountInterventionRequestModelFull
      val expected = DatabaseModels.accountInterventionEventFull
      SignalProcessorEventAdapters.V1.eventRequestToDbEvent(input) shouldBe expected
    }

    "transform a credential compromise request" in {
      val input = EventRequestV1Models.credentialCompromiseRequestModelFull
      val expected = DatabaseModels.credentialCompromiseEventFull
      SignalProcessorEventAdapters.V1.eventRequestToDbEvent(input) shouldBe expected
    }
  }

  "V2 eventRequestToDbEvent()" should {
    "transform a credential concern request" in {
      val input = EventRequestV2Models.credentialConcernRequestModel
      val expected = DatabaseModels.credentialConcernEventFull
      SignalProcessorEventAdapters.V2.eventRequestToDbEvent(input) shouldBe expected
    }

    "transform a device concern request" in {
      val input = EventRequestV2Models.deviceConcernRequestModel
      val expected = DatabaseModels.deviceConcernEventFull
      SignalProcessorEventAdapters.V2.eventRequestToDbEvent(input) shouldBe expected
    }
  }

  "determineTimeOfInterest()" should {
    "return the correct time of interest" in {
      val f = SignalProcessorEventAdapters.determineTimeOfInterest
      f(Some(1L), None, None) shouldBe Right(Instant.ofEpochMilli(1L))
      f(None, Some(2L), None) shouldBe Right(Instant.ofEpochMilli(2L))
      f(None, None, Some(3L)) shouldBe Right(Instant.ofEpochMilli(3L))
    }

    "return a Left when all timestamps are missing" in {
      val message = "EventData missing time of interest"
      SignalProcessorEventAdapters.determineTimeOfInterest(None, None, None) shouldBe Left(message)
    }
  }

  "getAction()" should {
    "get the action of an Account Intervention" in {
      val base = DatabaseModels.accountInterventionEventDataFull.asInstanceOf[AccountInterventionEventData]
      val active = base.copy(state = Some("active"), action = Some("testAction"))
      val suspended = base.copy(state = Some("suspended"))
      val permanentlySuspended = base.copy(state = Some("permanently_suspended"))
      val noState = base.copy(state = None)

      SignalProcessorEventAdapters.getAction(active)               shouldBe Some("testAction")
      SignalProcessorEventAdapters.getAction(suspended)            shouldBe Some("suspended")
      SignalProcessorEventAdapters.getAction(permanentlySuspended) shouldBe Some("permanently_suspended")
      SignalProcessorEventAdapters.getAction(noState)              shouldBe None
    }
  }

  "eventToEventDocument()" should {
    val now = Instant.now()
    val fixedClock = Clock.fixed(now, ZoneId.systemDefault())

    "transform an Event with valid timestamps" in {
      val event = DatabaseModels.accountConcernEventFull
      val expected = EventDocument(
        event          = event,
        storedAt       = now,
        timeOfInterest = Instant.ofEpochMilli(1507644997001L),
        action         = None
      )
      SignalProcessorEventAdapters.eventToEventDocument(event)(using fixedClock) shouldBe Right(expected)
    }

    "reject an Event with invalid timestamps" in {
      val baseEvent = DatabaseModels.accountConcernEventFull
      val eventData = baseEvent.eventData
        .asInstanceOf[AccountConcernEventData]
        .copy(
          eventTimestampMs = None,
          startTimeMs      = None,
          endTimeMs        = None
        )
      val event = baseEvent.copy(eventData = eventData)
      val message = "EventData missing time of interest"
      SignalProcessorEventAdapters.eventToEventDocument(event)(using fixedClock) shouldBe Left(message)
    }
  }
}
