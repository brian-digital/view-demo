/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.test

import play.api.libs.json.*

import java.time.*
import scala.language.implicitConversions

case class TestEventRequest(eventCount: Int,
                            dateFrom: Option[Instant] = None,
                            dateTo: Option[Instant] = None,
                            randomlyGenerate: Boolean = true,
                            updateView: Boolean = true,
                            v2EventsAvailable: Boolean = false
                           )

object TestEventRequest {

  private def testEventRequestValidatingReads(clock: Clock): Reads[TestEventRequest] = json =>
    for {
      eventCount <- (json \ "eventCount").validate[Int]
      dateFrom   <- (json \ "dateFrom").validateOpt[Instant]
      dateTo     <- (json \ "dateTo").validateOpt[Instant]
      (validDateFrom, validDateTo) <- (dateFrom, dateTo) match {
                                        case (Some(f), Some(t)) if f.isAfter(t) =>
                                          JsError("dateFrom must be equal to or before dateTo")
                                        case (Some(f), Some(t)) =>
                                          JsSuccess((Some(f), Some(t)))
                                        case (Some(f), None) =>
                                          JsSuccess((Some(f), None))
                                        case (None, Some(t)) =>
                                          JsSuccess((None, Some(t)))
                                        case (None, None) => JsSuccess((None, None))
                                      }
      randomlyGenerateOpt <- (json \ "randomlyGenerate").validateOpt[Boolean]
      randomlyGenerate = randomlyGenerateOpt.getOrElse(true)
      updateViewOpt <- (json \ "updateView").validateOpt[Boolean]
      updateView = updateViewOpt.getOrElse(true)
      v2EventsAvailableOpt <- (json \ "v2EventsAvailable").validateOpt[Boolean]
      v2EventsAvailable = v2EventsAvailableOpt.getOrElse(false)
    } yield TestEventRequest(eventCount, validDateFrom, validDateTo, randomlyGenerate, updateView, v2EventsAvailable)

  implicit def formats(clock: Clock): Format[TestEventRequest] =
    Format(testEventRequestValidatingReads(clock), Json.writes)

}
