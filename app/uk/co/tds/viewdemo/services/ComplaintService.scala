/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.services

import org.bson.types.ObjectId
import play.api.Logging
import uk.co.tds.viewdemo.config.FeatureToggles
import uk.co.tds.viewdemo.models.signalprocessor.v1.ComplaintRequest
import uk.co.tds.viewdemo.repositories.{ComplaintsRepository, SearchView}

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

enum InsertEventError:
  case FailedInsert
  case FailedViewUpdate

class ComplaintService @Inject()(eventDocumentRepository: ComplaintsRepository, gdsSearchView: SearchView, featureToggles: FeatureToggles)(using ExecutionContext, Clock) extends Logging {

  private def updateView(mongoId: ObjectId): Future[Either[String, Unit]] =
    gdsSearchView.updateMaterializedViewForSingleRecord(mongoId)

  def insertAndUpdateView(complaint: ComplaintRequest): Future[Either[InsertEventError, Unit]] =
    eventDocumentRepository.insert(complaint).flatMap {
      case Left(message) =>
        Future.successful(Left(InsertEventError.FailedInsert))
      case Right(mongoId) =>
        if (featureToggles.useViewEnabled) {
          updateView(mongoId).map {
            case Left(message) => Left(InsertEventError.FailedViewUpdate)
            case Right(unit)   => Right(unit)
          }
        } else {
          Future.successful(Right(()))
        }
    }
}
