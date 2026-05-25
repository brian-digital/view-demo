/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.controllers

import play.api.Logging
import play.api.libs.json.*
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.gov.hmrc.blockandsignalsgdsstore.controllers.actions.UserAgentFilter
import uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v1.EventRequestV1
import uk.gov.hmrc.blockandsignalsgdsstore.models.signalprocessor.v2.EventRequestV2
import uk.gov.hmrc.blockandsignalsgdsstore.services.{EventService, InsertEventError}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class EventController @Inject() (
  eventService: EventService,
  cc: ControllerComponents,
  userAgentFilter: UserAgentFilter
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

  def storeEvent(version: String): Action[JsValue] = (Action andThen userAgentFilter).async(parse.json) { request =>
    version match {
      case "v1" =>
        validateRequest[EventRequestV1](request)(using EventRequestV1.httpReads) match
          case Left(result) => Future.successful(result)
          case Right(eventRequest) =>
            eventService.insertAndUpdateViewV1(eventRequest).map {
              case Left(insertEventError) =>
                handleInsertFailure(
                  eventType        = eventRequest.metadata.signalsEventType.spValue,
                  jti              = eventRequest.metadata.jti,
                  insertEventError = insertEventError
                )
              case Right(_) => Created
            }
      case "v2" =>
        validateRequest[EventRequestV2](request)(using EventRequestV2.httpReads) match
          case Left(result) => Future.successful(result)
          case Right(eventRequest) =>
            eventService.insertAndUpdateViewV2(eventRequest).map {
              case Left(insertEventError) =>
                handleInsertFailure(
                  eventType        = eventRequest.metadata.signalsEventType.spValue,
                  jti              = eventRequest.metadata.jti,
                  insertEventError = insertEventError
                )
              case Right(_) => Created
            }
      case _ => Future.successful(BadRequest("Only :version v1 and v2 are supported"))
    }
  }
}
