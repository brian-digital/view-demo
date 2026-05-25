/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.services

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.{SearchRequest, SearchResult}
import uk.gov.hmrc.blockandsignalsgdsstore.repositories.EventDocumentRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SearchService @Inject() (eventDocumentRepository: EventDocumentRepository)(implicit ec: ExecutionContext) {
  def find(searchRequest: SearchRequest): Source[SearchResult, NotUsed] = {
    eventDocumentRepository.find(searchRequest)
  }

  def count(searchRequest: SearchRequest): Future[Long] = {
    eventDocumentRepository.count(searchRequest)
  }
}
