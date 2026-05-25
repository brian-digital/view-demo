/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.services

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.{SearchRequest, SearchResultV2}

import scala.concurrent.Future

trait CountAndSearch {
  def find(searchRequest: SearchRequest): Source[SearchResultV2, NotUsed]

  def count(searchRequest: SearchRequest): Future[Long]
}
