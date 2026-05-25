/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.services

import org.bson.types.ObjectId
import play.api.Logging
import uk.gov.hmrc.blockandsignalsgdsstore.adapters.SignalProcessorEventAdapters
import uk.gov.hmrc.blockandsignalsgdsstore.config.FeatureToggles
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.{Event, EventDocument}
import uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v1.EventRequestV1
import uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v2.EventRequestV2
import uk.gov.hmrc.blockandsignalsgdsstore.repositories.{EventDocumentRepository, GDSSearchView}

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

enum InsertEventError:
  case FailedInsert
  case FailedViewUpdate

class EventService @Inject() (eventDocumentRepository: EventDocumentRepository, gdsSearchView: GDSSearchView, featureToggles: FeatureToggles)(using ExecutionContext, Clock) extends Logging {

  private def insertEvent(event: Event): Future[Either[String, ObjectId]] = {
    val eventDocumentEither = SignalProcessorEventAdapters.eventToEventDocument(event)
    eventDocumentEither match
      case Left(message)        => Future.successful(Left(message))
      case Right(eventDocument) => eventDocumentRepository.insert(eventDocument)
  }

  private def updateView(mongoId: ObjectId): Future[Either[String, Unit]] =
    gdsSearchView.updateMaterializedViewForSingleRecord(mongoId)

  private def insertAndUpdateView(event: Event): Future[Either[InsertEventError, Unit]] =
    insertEvent(event).flatMap {
      case Left(message) =>
        Future.successful(Left(InsertEventError.FailedInsert))
      case Right(mongoId) =>
        if (featureToggles.v1SearchUseViewEnabled) {
          updateView(mongoId).map {
            case Left(message) => Left(InsertEventError.FailedViewUpdate)
            case Right(unit)   => Right(unit)
          }
        } else {
          Future.successful(Right(()))
        }
    }

  def insertAndUpdateViewV1(eventRequest: EventRequestV1): Future[Either[InsertEventError, Unit]] = {
    val event = SignalProcessorEventAdapters.V1.eventRequestToDbEvent(eventRequest)
    insertAndUpdateView(event)
  }

  def insertAndUpdateViewV2(eventRequest: EventRequestV2): Future[Either[InsertEventError, Unit]] = {
    val event = SignalProcessorEventAdapters.V2.eventRequestToDbEvent(eventRequest)
    insertAndUpdateView(event)
  }
}
