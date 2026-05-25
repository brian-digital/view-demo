/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.models.search

import play.api.libs.json.{Format, Json, Reads, Writes}
import uk.co.tds.viewdemo.models.common.InitiatingEntity
import uk.co.tds.viewdemo.models.db.ComplaintType
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class SearchResult(
                           subject: String,
                           complaintType: ComplaintType,
                           initiator: String,
                           userId: String,
                           submittedBy: Option[InitiatingEntity],
                           reason: Option[String],
                           submittedOn: Instant,
                           storedAt: Instant
)

object SearchResult {

  val mongoFormat: Format[SearchResult] = {
    given Format[Instant] = MongoJavatimeFormats.instantFormat
    given Format[ComplaintType] = ComplaintType.mongoFormat
    Json.format[SearchResult]
  }

  val httpFormat: Format[SearchResult] = {
    given Format[Instant] = Format(Reads.DefaultInstantReads, Writes.DefaultInstantWrites)
    given Format[ComplaintType] = Format(ComplaintType.searchValueReads, ComplaintType.searchValueWrites)
    Json.format[SearchResult]
  }
}
