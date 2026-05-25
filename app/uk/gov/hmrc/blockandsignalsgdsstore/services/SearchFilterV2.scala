/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.services

import org.bson.conversions.Bson
import org.mongodb.scala.model.Filters
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.InitiatingEntity
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.EventType
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.SearchRequest

import java.time.ZoneOffset

class SearchFilterV2 {

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

    // Always enforce AccountIntervention must be Analyst
    val accountInterventionAnalystCondition: Bson = Filters.and(
      Filters.equal("eventType", EventType.AccountIntervention.mongoValue),
      Filters.equal("submittedBy", InitiatingEntity.Analyst.toString.toLowerCase)
    )

    val eventTypeFilter: Bson = {
      searchRequest.eventType match {
        case Some(EventType.AccountIntervention) => accountInterventionAnalystCondition
        case Some(eventType)                     => Filters.equal("eventType", eventType)
        case None                                =>
          // For no event type filter, include all event types
          // but ensure AccountIntervention only shows Analyst-initiated
          Filters.or(
            Filters.notEqual("eventType", EventType.AccountIntervention.mongoValue),
            accountInterventionAnalystCondition
          )
      }
    }

    val mandatoryFilters: Seq[Bson] = timeFilters :+ eventTypeFilter

    val optionalFilters: Seq[Bson] = Seq(
      searchRequest.credIds.map(ids => Filters.in("hmrcCredentialId", ids*)),
      searchRequest.subjectIds.map(ids => Filters.in("oneLoginSubjectId", ids*))
    ).flatten

    Filters.and((optionalFilters ++ mandatoryFilters)*)

  }
}
