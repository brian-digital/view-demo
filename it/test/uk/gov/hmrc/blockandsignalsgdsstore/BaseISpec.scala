/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.Injecting
import uk.gov.hmrc.blockandsignalsgdsstore.config.EventStoreConfig
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.FiniteDuration

trait BaseISpec extends AnyWordSpec with BeforeAndAfterEach with GuiceOneServerPerSuite with Matchers with ScalaFutures with IntegrationPatience with Injecting {

  implicit val executionContext: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val hostURL = s"http://localhost:$port"
  val baseUrl = s"$hostURL/block-and-signals-gds-store"
  val eventStoreConfig: EventStoreConfig = EventStoreConfig(
    searchResultLimit                 = 100,
    eventTtl                          = FiniteDuration(1, "day"),
    searchResultsChunkSize            = 500,
    searchResultsChunkWindow          = FiniteDuration(150, "millis"),
    searchRequestMaxAllowedIds        = 20,
    searchRequestMaxAllowedCredIds    = 20,
    searchRequestMaxAllowedSubjectIds = 20,
    mongoSearchResultBatchSize        = 1000
  )
  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .build()

}
