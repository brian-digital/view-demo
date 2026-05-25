/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.services

import javax.inject.{Inject, Singleton}
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import uk.co.tds.viewdemo.models.search.{SearchRequest, SearchResult}
import uk.co.tds.viewdemo.repositories.SearchView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SearchService @Inject()(
  complaintSearchView: SearchView
)(using ec: ExecutionContext)
    extends CountAndSearch {
  override def find(searchRequest: SearchRequest): Source[SearchResult, NotUsed] = complaintSearchView.find(searchRequest)

  override def count(searchRequest: SearchRequest): Future[Long] = complaintSearchView.count(searchRequest)
}
