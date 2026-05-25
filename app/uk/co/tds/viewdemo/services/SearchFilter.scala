/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.services

import org.bson.conversions.Bson
import org.mongodb.scala.model.Filters
import play.api.libs.json.{Reads, Writes}
import uk.co.tds.viewdemo.models.common.InitiatingEntity
import uk.co.tds.viewdemo.models.db.ComplaintType
import uk.co.tds.viewdemo.models.search.SearchRequest

import java.time.ZoneOffset

class SearchFilter {

  def buildFilter(searchRequest: SearchRequest): Bson = {

    // dateFrom: 2025-01-25
    // 2025-01-25T00:00:00Z
    val dateFromInstant = searchRequest.dateFrom
      .atStartOfDay(ZoneOffset.UTC)
      .toInstant

    // dateTo: 2025-01-25
    // 2025-01-25T23:59:59.999999999Z
    val dateToInstant = searchRequest.dateTo
      .plusDays(1)
      .atStartOfDay(ZoneOffset.UTC)
      .minusNanos(1)
      .toInstant

    val timeFilters: Seq[Bson] = Seq(
      Filters.gte("submittedOn", dateFromInstant),
      Filters.lte("submittedOn", dateToInstant)
    )


    val mandatoryFilters: Seq[Bson] = timeFilters

//    Filters.lte("submittedOn", dateToInstant)
    Filters.empty()
  }
}
