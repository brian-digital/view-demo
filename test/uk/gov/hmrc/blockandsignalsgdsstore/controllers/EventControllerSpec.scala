/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.blockandsignalsgdsstore.controllers.actions.UserAgentFilter
import uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v1.EventRequestV1
import uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v2.EventRequestV2
import uk.gov.hmrc.blockandsignalsgdsstore.services.{EventService, InsertEventError}
import uk.gov.hmrc.blockandsignalsgdsstore.utils.TestData

import scala.concurrent.ExecutionContext.Implicits.global as ec
import scala.concurrent.{ExecutionContext, Future}

class EventControllerSpec extends AnyWordSpec with Matchers with TestData {

  private def fakePostRequest[T](body: T): FakeRequest[T] = FakeRequest("POST", "/").withBody(body)

  trait Setup {
    val mockEventService: EventService = Mockito.mock(classOf[EventService])

    val mockUserAgentFilter: UserAgentFilter = new UserAgentFilter {
      override protected def filter[A](request: Request[A]): Future[Option[Result]] = Future.successful(None)

      override protected def executionContext: ExecutionContext = ec
    }

    val mockConfiguration: Configuration = Mockito.mock(classOf[Configuration])

    val eventController: EventController = EventController(
      mockEventService,
      Helpers.stubControllerComponents(),
      mockUserAgentFilter
    )
  }

  "POST /:version/event" should {

    "return 400 when the :version is not valid" in new Setup {
      val result: Future[Result] =
        eventController.storeEvent("invalid")(fakePostRequest(EventRequestV1Json.accountConcernJsonFull))
      status(result)          shouldBe Status.BAD_REQUEST
      contentAsString(result) shouldBe "Only :version v1 and v2 are supported"
    }
  }

  "POST /v1/event" should {

    "return 201, when receive an Account Concern event" in new Setup {
      when(mockEventService.insertAndUpdateViewV1(any[EventRequestV1])).thenReturn(Future.successful(Right(())))
      val result: Future[Result] =
        eventController.storeEvent("v1")(fakePostRequest(EventRequestV1Json.accountConcernJsonFull))
      status(result) shouldBe Status.CREATED
    }

    "return 201, when receive an Account Intervention even" in new Setup {
      when(mockEventService.insertAndUpdateViewV1(any[EventRequestV1])).thenReturn(Future.successful(Right(())))
      val result: Future[Result] =
        eventController.storeEvent("v1")(fakePostRequest(EventRequestV1Json.accountInterventionJsonFull))
      status(result) shouldBe Status.CREATED
    }

    "return 500 when the insert fails" in new Setup {
      when(mockEventService.insertAndUpdateViewV1(any[EventRequestV1]))
        .thenReturn(Future.successful(Left(InsertEventError.FailedInsert)))

      val result: Future[Result] =
        eventController.storeEvent("v1")(fakePostRequest(EventRequestV1Json.accountConcernJsonFull))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      contentAsString(result) should include(
        "Unable to insert event. EventType: accountConcern, eventId: 756E69717565206964656E746966696572"
      )
    }

    "return 500 when the view update fails" in new Setup {
      when(mockEventService.insertAndUpdateViewV1(any[EventRequestV1]))
        .thenReturn(Future.successful(Left(InsertEventError.FailedViewUpdate)))

      val result: Future[Result] =
        eventController.storeEvent("v1")(fakePostRequest(EventRequestV1Json.accountConcernJsonFull))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      contentAsString(result) should include(
        "Unable to update view for event. EventType: accountConcern, eventId: 756E69717565206964656E746966696572"
      )
    }

    "return 400, when an invalid payload is received" in new Setup {
      val result: Future[Result] = eventController.storeEvent("v1")(fakePostRequest(EventRequestV1Json.unknownEventJson))
      status(result)          shouldBe Status.BAD_REQUEST
      contentAsString(result) shouldBe "Invalid payload: Missing `/metadata/signalsEventType`"
    }
  }

  "POST /v2/event" should {
    "return 201 Created" when {
      "the event is a credential concern" in new Setup {
        when(mockEventService.insertAndUpdateViewV2(any[EventRequestV2])).thenReturn(Future.successful(Right(())))
        val result: Future[Result] =
          eventController.storeEvent("v2")(fakePostRequest(EventRequestV2Json.credentialConcernJsonFull))
        status(result) shouldBe Status.CREATED
      }

      "the event is a device concern" in new Setup {
        when(mockEventService.insertAndUpdateViewV2(any[EventRequestV2])).thenReturn(Future.successful(Right(())))
        val result: Future[Result] =
          eventController.storeEvent("v2")(fakePostRequest(EventRequestV2Json.deviceConcernJsonFull))
        status(result) shouldBe Status.CREATED
      }
    }

    "return 500 Internal Server Error" when {
      "the insert fails" in new Setup {
        when(mockEventService.insertAndUpdateViewV2(any[EventRequestV2]))
          .thenReturn(Future.successful(Left(InsertEventError.FailedInsert)))

        val result: Future[Result] =
          eventController.storeEvent("v2")(fakePostRequest(EventRequestV2Json.credentialConcernJsonFull))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        contentAsString(result) should include(
          "Unable to insert event. EventType: credentialConcern, eventId: 756E69717565206964656E746966696572"
        )
      }

      "the view update fails" in new Setup {
        when(mockEventService.insertAndUpdateViewV2(any[EventRequestV2]))
          .thenReturn(Future.successful(Left(InsertEventError.FailedViewUpdate)))

        val result: Future[Result] =
          eventController.storeEvent("v2")(fakePostRequest(EventRequestV2Json.credentialConcernJsonFull))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        contentAsString(result) should include(
          "Unable to update view for event. EventType: credentialConcern, eventId: 756E69717565206964656E746966696572"
        )
      }
    }

    "return 400" when {
      "the payload is invalid" in new Setup {
        val invalidJson: JsObject = Json.obj("invalid" -> "request")
        val result: Future[Result] = eventController.storeEvent("v1")(fakePostRequest(invalidJson))
        status(result)        shouldBe Status.BAD_REQUEST
        contentAsString(result) should include("Invalid payload:")
      }
    }
  }
}
