/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.controllers

import javax.inject.{Inject, Singleton}
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.libs.json.*
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.blockandsignalsgdsstore.config.EventStoreConfig
import uk.gov.hmrc.blockandsignalsgdsstore.controllers.actions.UserAgentFilter
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.{SearchRequest, SearchResultV2}
import uk.gov.hmrc.blockandsignalsgdsstore.services.{CountAndSearch, SearchServiceV2}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VersionedSearchController @Inject() (
  cc: ControllerComponents,
  searchServiceV2: SearchServiceV2,
  eventStoreConfig: EventStoreConfig,
  userAgentFilter: UserAgentFilter
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  implicit val searchRequestFormat: Format[SearchRequest] = SearchRequest.searchRequestFormat(eventStoreConfig)

  private def searchVersion(version: String): Option[CountAndSearch] =
    version match {
      case "v2" =>
        Some(searchServiceV2)
      case _ => None
    }

  def search(version: String): Action[JsValue] = (Action andThen userAgentFilter).async(parse.json) { implicit request =>
    request.body.validate[SearchRequest] match {
      case JsSuccess(searchRequest, _) =>
        searchVersion(version).fold {
          logger.info(s"INVALID_SEARCH_VERSION: $version is not a valid search version")
          Future.successful(NotImplemented(s"search version $version not implemented"))
        } { searchService =>
          for {
            searchCount <- searchServiceV2.count(searchRequest)
            result <- if (searchCount > eventStoreConfig.searchResultLimit) {
                        logger.info(s"######### search count: $searchCount")
                        Future.successful(ExpectationFailed(Json.obj("count" -> searchCount)))
                      } else if (searchCount == 0) {
                        Future.successful(NoContent)
                      } else {
                        logger.info(s"######### search count: $searchCount")
                        val groupedChunks = searchServiceV2
                          .find(searchRequest)
                          .map { searchResult =>
                            Json.stringify(Json.toJson(searchResult)(using SearchResultV2.httpFormat)) + "\n"
                          }
                          .async
                          .groupedWithin(eventStoreConfig.searchResultsChunkSize, eventStoreConfig.searchResultsChunkWindow)
                          .map(group => ByteString(group.mkString))

                        Future.successful(Ok.chunked(groupedChunks, contentType = Some("application/x-ndjson")))
                      }
          } yield result
        }
      case JsError(errors) =>
        logger.info(s"INVALID_SEARCH_BODY: ${errors.flatMap(_._2).map(_.message)}")
        Future.successful(BadRequest(s"""Invalid payload:`${errors.flatMap(_._2).map(_.message)}`"""))
    }
  }

  def count(version: String): Action[JsValue] = (Action andThen userAgentFilter).async(parse.json) { implicit request =>
    request.body.validate[SearchRequest] match {
      case JsSuccess(searchRequest, _) =>
        searchVersion(version).fold {
          logger.info(s"INVALID_SEARCH_VERSION: $version is not a valid search version")
          Future.successful(NotImplemented(s"search version $version not implemented"))
        } { searchService =>
          searchService
            .count(searchRequest)
            .map(count =>
              logger.info(s"!!!!!!!!!! searchCount: $count")
              Ok(Json.obj("count" -> count))
            )
        }
      case JsError(errors) =>
        logger.info(s"INVALID_SEARCH_BODY: ${errors.flatMap(_._2).map(_.message)}")
        Future.successful(BadRequest(s"""Invalid payload:`${errors.flatMap(_._2).map(_.message)}`"""))
    }
  }
}
