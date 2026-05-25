/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.models.search

import play.api.libs.json.*
import uk.co.tds.viewdemo.config.ComplaintsConfig
import uk.co.tds.viewdemo.models.db.ComplaintType

import java.time.LocalDate

case class SearchRequest(dateFrom: LocalDate, dateTo: LocalDate)

object SearchRequest {

  private def searchRequestValidatingReads(eventStoreConfig: ComplaintsConfig): Reads[SearchRequest] = json => {
    for {
      dateFrom <- (json \ "dateFrom").validate[LocalDate]
      dateTo   <- (json \ "dateTo").validate[LocalDate]
      validatedDateFrom <- if (!dateFrom.isAfter(dateTo)) {
                             JsSuccess(dateFrom)
                           } else {
                             JsError("dateFrom must be equal to or before dateTo")
                           }
      validateDateTo <- if (!dateTo.isBefore(dateFrom)) {
                          JsSuccess(dateTo)
                        } else {
                          JsError("dateTo must be equal to or after dateFrom")
                        }

    } yield SearchRequest(dateFrom = validatedDateFrom, dateTo = validateDateTo)
  }

  implicit val eventTypeWrites: Writes[ComplaintType] = ComplaintType.searchValueWrites

  def searchRequestFormat(eventStoreConfig: ComplaintsConfig): Format[SearchRequest] =
    Format(searchRequestValidatingReads(eventStoreConfig), Json.writes)

}
