/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.controllers

import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.libs.json.*
import play.api.mvc.{Action, ControllerComponents}
import uk.co.tds.viewdemo.config.ComplaintsConfig
import uk.co.tds.viewdemo.models.search.{SearchRequest, SearchResult}
import uk.co.tds.viewdemo.services.SearchService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SearchController @Inject(cc: ControllerComponents, searchService: SearchService, eventStoreConfig: ComplaintsConfig)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  implicit val searchRequestFormat: Format[SearchRequest] = SearchRequest.searchRequestFormat(eventStoreConfig)

  def search(): Action[JsValue] = Action.async(parse.json) { implicit request =>
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
                          Json.stringify(Json.toJson(searchResult)(using SearchResult.httpFormat)) + "\n"
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

  def count(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[SearchRequest] match {
      case JsSuccess(searchRequest, _) =>
        searchService.count(searchRequest).map(count => Ok(Json.obj("count" -> count)))
      case JsError(errors) =>
        logger.info(s"INVALID_SEARCH_BODY: ${errors.flatMap(_._2).map(_.message)}")
        Future.successful(BadRequest(s"""Invalid payload:`${errors.flatMap(_._2).map(_.message)}`"""))
    }
  }

}
