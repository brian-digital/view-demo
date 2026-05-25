/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.services

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import uk.co.tds.viewdemo.models.search.{SearchRequest, SearchResult}

import scala.concurrent.Future

trait CountAndSearch {
  def find(searchRequest: SearchRequest): Source[SearchResult, NotUsed]

  def count(searchRequest: SearchRequest): Future[Long]
}
