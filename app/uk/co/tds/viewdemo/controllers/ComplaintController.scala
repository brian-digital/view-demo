/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.controllers

import play.api.Logging
import play.api.libs.json.*
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.co.tds.viewdemo.models.signalprocessor.v1.ComplaintRequest
import uk.co.tds.viewdemo.services.{ComplaintService, InsertEventError}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class ComplaintController @Inject()(
                                  eventService: ComplaintService,
                                  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  private def handleInvalidPayload(jsError: JsError): Result = {
    val errorsAsString = jsError.errors.map(_._1).mkString(", ")
    logger.warn(s"Invalid payload $errorsAsString")
    BadRequest(s"Invalid payload: Missing `$errorsAsString`")
  }

  private def handleInvalidBody(exception: RuntimeException): Result = {
    val errorMessage = exception.getMessage
    logger.warn(s"Exception: $errorMessage")
    BadRequest(s"Invalid body: $errorMessage")
  }

  private def validateRequest[T](request: Request[JsValue])(using Reads[T]): Either[Result, T] = {
    try
      request.body.validate[T] match
        case JsSuccess(value, _) => Right(value)
        case jsError: JsError    => Left(handleInvalidPayload(jsError))
    catch case error: RuntimeException => Left(handleInvalidBody(error))
  }

  private def handleInsertFailure(eventType: String, jti: String, insertEventError: InsertEventError): Result = {
    val messagePrefix = insertEventError match
      case InsertEventError.FailedInsert     => "Unable to insert event."
      case InsertEventError.FailedViewUpdate => "Unable to update view for event."
    val messageDetails = s"EventType: $eventType, eventId: $jti"
    val message: String = s"$messagePrefix $messageDetails"
    logger.error(message)
    InternalServerError(message)
  }

  def storeEvent(): Action[JsValue] = Action.async(parse.json) { request =>
    validateRequest[ComplaintRequest](request)(using ComplaintRequest.httpReads) match
      case Left(result) => Future.successful(result)
      case Right(complaintRequest) =>
        eventService.insertAndUpdateView(complaintRequest).map {
          case Left(insertEventError) =>
            handleInsertFailure(
              eventType = complaintRequest.metadata.complaintType.spValue,
              jti = complaintRequest.metadata.complaintRef,
              insertEventError = insertEventError
            )
          case Right(_) => Created
        }
  }
}
