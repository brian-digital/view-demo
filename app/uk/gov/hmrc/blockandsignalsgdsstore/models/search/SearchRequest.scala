/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.search

import play.api.libs.json.*
import uk.gov.hmrc.blockandsignalsgdsstore.config.EventStoreConfig
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.EventType

import java.time.LocalDate

case class SearchRequest(dateFrom: LocalDate, dateTo: LocalDate, credIds: Option[Seq[String]], subjectIds: Option[Seq[String]], eventType: Option[EventType])

object SearchRequest {

  private def searchRequestValidatingReads(eventStoreConfig: EventStoreConfig): Reads[SearchRequest] = json => {
    val maxAllowedIds = eventStoreConfig.searchRequestMaxAllowedIds
    val maxAllowedCredIds = eventStoreConfig.searchRequestMaxAllowedCredIds
    val maxAllowedSubjectIds = eventStoreConfig.searchRequestMaxAllowedSubjectIds

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
      eventType <- (json \ "eventType").validateOpt[EventType](EventType.searchValueReads)

      credIds    <- (json \ "credIds").validateOpt[Seq[String]]
      subjectIds <- (json \ "subjectIds").validateOpt[Seq[String]]

      validCreds <- (credIds, subjectIds) match {
                      case (Some(c), Some(s)) if c.size + s.size > maxAllowedIds =>
                        JsError(s"credIds and subjectIds must be less than $maxAllowedIds")
                      case (Some(c), _) if c.size > maxAllowedCredIds =>
                        JsError(s"credIds must be less than $maxAllowedCredIds")
                      case (c, _) => JsSuccess(c)
                    }

      validSubjects <- (credIds, subjectIds) match {
                         case (Some(c), Some(s)) if c.size + s.size > maxAllowedIds =>
                           JsError(s"credIds and subjectIds must be less than $maxAllowedIds")
                         case (_, Some(s)) if s.size > maxAllowedSubjectIds =>
                           JsError(s"subjectIds must be less than $maxAllowedSubjectIds")
                         case (_, s) => JsSuccess(s)
                       }

    } yield SearchRequest(dateFrom = validatedDateFrom, dateTo = validateDateTo, credIds = validCreds, subjectIds = validSubjects, eventType = eventType)
  }

  implicit val eventTypeWrites: Writes[EventType] = EventType.searchValueWrites

  def searchRequestFormat(eventStoreConfig: EventStoreConfig): Format[SearchRequest] =
    Format(searchRequestValidatingReads(eventStoreConfig), Json.writes)

}
