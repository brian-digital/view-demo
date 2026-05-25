/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.test

import play.api.Logging
import play.api.libs.json.{Format, JsError, JsSuccess, JsValue}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.blockandsignalsgdsstore.config.AppConfig
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, LocalDate}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestEventController @Inject (cc: ControllerComponents, testEventGeneratorService: TestEventGeneratorService, clock: Clock, appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  implicit val formats: Format[TestEventRequest] = TestEventRequest.formats(clock)
  private val defaultFrom = LocalDate.now(clock).atStartOfDay(clock.getZone).minusMonths(18).toInstant
  private val defaultTo = LocalDate.now(clock).plusDays(1).atStartOfDay(clock.getZone).minusNanos(1).toInstant

  def create(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[TestEventRequest] match {
      case JsSuccess(testEventRequest, _) =>
        testEventGeneratorService
          .generateTestEvents(
            testEventRequest.eventCount,
            testEventRequest.dateFrom.getOrElse(defaultFrom),
            testEventRequest.dateTo.getOrElse(defaultTo),
            testEventRequest.randomlyGenerate
          )
          .map(executed => Created)
      case JsError(errors) =>
        logger.warn(s"Bad Request:Invalid payload: ${errors.flatMap(_._2).map(_.message)}")
        Future.successful(BadRequest(s"""Invalid payload:`${errors.flatMap(_._2).map(_.message)}`"""))
    }
  }

  def clearDBUsingTTL(): Action[AnyContent] = Action.async { implicit request =>
    if (appConfig.featureToggles.v1SearchUseViewEnabled)
      testEventGeneratorService.clearDBSettingTTLRecreateView().map(_ => NoContent)
      testEventGeneratorService.clearViewSettingTTL().map(_ => NoContent)
    else testEventGeneratorService.clearDBSettingTTL().map(_ => NoContent)
  }

  def delete(): Action[AnyContent] = Action.async { implicit request =>
    if (appConfig.featureToggles.v1SearchUseViewEnabled)
      testEventGeneratorService.deleteTestEventsAndView().map(_ => NoContent)
    else
      testEventGeneratorService.deleteTestEvents().map(_ => NoContent)
  }

  def dropViewAndRecreate(): Action[AnyContent] = Action.async {
    testEventGeneratorService.dropViewAndRecreate().map(_ => NoContent)
  }
}
