/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.controllers

import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.libs.json.*
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.blockandsignalsgdsstore.config.EventStoreConfig
import uk.gov.hmrc.blockandsignalsgdsstore.controllers.actions.UserAgentFilter
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.{SearchRequest, SearchResult}
import uk.gov.hmrc.blockandsignalsgdsstore.services.SearchService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SearchController @Inject (cc: ControllerComponents, searchService: SearchService, eventStoreConfig: EventStoreConfig, userAgentFilter: UserAgentFilter)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  implicit val searchResultWrites: Writes[SearchResult] = SearchResult.httpWrites
  implicit val searchRequestFormat: Format[SearchRequest] = SearchRequest.searchRequestFormat(eventStoreConfig)

  def search(): Action[JsValue] = (Action andThen userAgentFilter).async(parse.json) { implicit request =>
    request.body.validate[SearchRequest] match {
      case JsSuccess(searchRequest, _) =>
        for {
          searchCount <- searchService.count(searchRequest)
          result <- if (searchCount > eventStoreConfig.searchResultLimit) {
                      Future.successful(ExpectationFailed(Json.obj("count" -> searchCount)))
                    } else if (searchCount == 0) {
                      Future.successful(NoContent)
                    } else {
                      val groupedChunks = searchService
                        .find(searchRequest)
                        .map { searchResult =>
                          Json.stringify(Json.toJson(searchResult)) + "\n"
                        }
                        .async
                        .groupedWithin(eventStoreConfig.searchResultsChunkSize, eventStoreConfig.searchResultsChunkWindow)
                        .map(group => ByteString(group.mkString))

                      Future.successful(Ok.chunked(groupedChunks, contentType = Some("application/x-ndjson")))
                    }
        } yield result
      case JsError(errors) =>
        logger.info(s"INVALID_SEARCH_BODY: ${errors.flatMap(_._2).map(_.message)}")
        Future.successful(BadRequest(s"""Invalid payload:`${errors.flatMap(_._2).map(_.message)}`"""))
    }
  }

  def count(): Action[JsValue] = (Action andThen userAgentFilter).async(parse.json) { implicit request =>
    request.body.validate[SearchRequest] match {
      case JsSuccess(searchRequest, _) =>
        searchService.count(searchRequest).map(count => Ok(Json.obj("count" -> count)))
      case JsError(errors) =>
        logger.info(s"INVALID_SEARCH_BODY: ${errors.flatMap(_._2).map(_.message)}")
        Future.successful(BadRequest(s"""Invalid payload:`${errors.flatMap(_._2).map(_.message)}`"""))
    }
  }

}
