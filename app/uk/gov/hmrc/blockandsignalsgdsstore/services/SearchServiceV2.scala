/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.services

import javax.inject.{Inject, Singleton}
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.{SearchRequest, SearchResultV2}
import uk.gov.hmrc.blockandsignalsgdsstore.repositories.GDSSearchView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SearchServiceV2 @Inject() (
  gdsSearchView: GDSSearchView
)(using ec: ExecutionContext)
    extends CountAndSearch {
  override def find(searchRequest: SearchRequest): Source[SearchResultV2, NotUsed] = gdsSearchView.find(searchRequest)

  override def count(searchRequest: SearchRequest): Future[Long] = gdsSearchView.count(searchRequest)
}
