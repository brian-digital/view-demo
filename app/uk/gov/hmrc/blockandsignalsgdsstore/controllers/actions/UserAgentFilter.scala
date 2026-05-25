/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.controllers.actions

import play.api.http.HeaderNames
import play.api.mvc.Results.Forbidden
import play.api.mvc.{ActionFilter, Request, Result}
import uk.gov.hmrc.blockandsignalsgdsstore.config.AppConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait UserAgentFilter extends ActionFilter[Request]

class UserAgentFilterImpl @Inject (appConfig: AppConfig)(implicit ec: ExecutionContext) extends UserAgentFilter {

  override protected def executionContext: ExecutionContext = ec

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = Future.successful {
    request.headers.get(HeaderNames.USER_AGENT) match {
      case Some(userAgent) if appConfig.userAgentAllowList.contains(userAgent) =>
        None
      case _ =>
        Some(Forbidden)
    }
  }

}
