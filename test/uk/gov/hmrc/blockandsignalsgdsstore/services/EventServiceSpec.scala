/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.services

import org.bson.types.ObjectId
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.blockandsignalsgdsstore.adapters.SignalProcessorEventAdapters
import uk.gov.hmrc.blockandsignalsgdsstore.config.FeatureToggles
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.{Event, EventDocument}
import uk.gov.hmrc.blockandsignalsgdsstore.repositories.{EventDocumentRepository, GDSSearchView}
import uk.gov.hmrc.blockandsignalsgdsstore.utils.TestData

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EventServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with TestData {

  private val now = Instant.now()
  given Clock = Clock.fixed(now, ZoneId.systemDefault())

  trait Setup {
    val mockGdsSearchView: GDSSearchView = mock[GDSSearchView]
    val mockRepo: EventDocumentRepository = mock[EventDocumentRepository]
    val mockToggles: FeatureToggles = mock[FeatureToggles]
    val service: EventService = EventService(mockRepo, mockGdsSearchView, mockToggles)

    val mongoId = ObjectId()
  }

  private def eventToDocument(event: Event): EventDocument =
    SignalProcessorEventAdapters.eventToEventDocument(event).toOption.get

  "insertAndUpdateViewV1()" should {
    val eventRequest = EventRequestV1Models.accountConcernRequestModelFull
    val expectedEvent = SignalProcessorEventAdapters.V1.eventRequestToDbEvent(eventRequest)
    val expectedDocument = eventToDocument(expectedEvent)

    "insert a record and update the view if the view toggle is on" in new Setup {
      when(mockToggles.v1SearchUseViewEnabled).thenReturn(true)
      when(mockRepo.insert(expectedDocument)).thenReturn(Future.successful(Right(mongoId)))
      when(mockGdsSearchView.updateMaterializedViewForSingleRecord(mongoId)).thenReturn(Future.successful(Right(())))
      service.insertAndUpdateViewV1(eventRequest).futureValue shouldBe Right(())
    }

    "insert a record only if the view toggle is off" in new Setup {
      when(mockToggles.v1SearchUseViewEnabled).thenReturn(false)
      when(mockRepo.insert(expectedDocument)).thenReturn(Future.successful(Right(mongoId)))
      service.insertAndUpdateViewV1(eventRequest).futureValue shouldBe Right(())
    }

    "handle an insert failure" in new Setup {
      when(mockToggles.v1SearchUseViewEnabled).thenReturn(false)
      when(mockRepo.insert(expectedDocument)).thenReturn(Future.successful(Left("error")))
      service.insertAndUpdateViewV1(eventRequest).futureValue shouldBe Left(InsertEventError.FailedInsert)
    }

    "handle a view update failure" in new Setup {
      when(mockToggles.v1SearchUseViewEnabled).thenReturn(true)
      when(mockRepo.insert(expectedDocument)).thenReturn(Future.successful(Right(mongoId)))
      when(mockGdsSearchView.updateMaterializedViewForSingleRecord(mongoId))
        .thenReturn(Future.successful(Left("error")))
      service.insertAndUpdateViewV1(eventRequest).futureValue shouldBe Left(InsertEventError.FailedViewUpdate)
    }
  }

  "insertAndUpdateViewV2()" should {
    val eventRequest = EventRequestV2Models.credentialConcernRequestModel
    val expectedEvent = SignalProcessorEventAdapters.V2.eventRequestToDbEvent(eventRequest)
    val expectedDocument = eventToDocument(expectedEvent)

    "insert a record and update the view if the view toggle is on" in new Setup {
      when(mockToggles.v1SearchUseViewEnabled).thenReturn(true)
      when(mockRepo.insert(expectedDocument)).thenReturn(Future.successful(Right(mongoId)))
      when(mockGdsSearchView.updateMaterializedViewForSingleRecord(mongoId)).thenReturn(Future.successful(Right(())))
      service.insertAndUpdateViewV2(eventRequest).futureValue shouldBe Right(())
    }

    "insert a record only if the view toggle is off" in new Setup {
      when(mockToggles.v1SearchUseViewEnabled).thenReturn(false)
      when(mockRepo.insert(expectedDocument)).thenReturn(Future.successful(Right(mongoId)))
      service.insertAndUpdateViewV2(eventRequest).futureValue shouldBe Right(())
    }

    "handle an insert failure" in new Setup {
      when(mockToggles.v1SearchUseViewEnabled).thenReturn(false)
      when(mockRepo.insert(expectedDocument)).thenReturn(Future.successful(Left("error")))
      service.insertAndUpdateViewV2(eventRequest).futureValue shouldBe Left(InsertEventError.FailedInsert)
    }

    "handle a view update failure" in new Setup {
      when(mockToggles.v1SearchUseViewEnabled).thenReturn(true)
      when(mockRepo.insert(expectedDocument)).thenReturn(Future.successful(Right(mongoId)))
      when(mockGdsSearchView.updateMaterializedViewForSingleRecord(mongoId))
        .thenReturn(Future.successful(Left("error")))
      service.insertAndUpdateViewV2(eventRequest).futureValue shouldBe Left(InsertEventError.FailedViewUpdate)
    }
  }
}
