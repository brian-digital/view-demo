/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.config

import play.api.Configuration

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration

@Singleton
class AppConfig @Inject() (config: Configuration) {

  val eventStoreConfig: EventStoreConfig = EventStoreConfig(
    eventTtl                          = config.get[FiniteDuration]("event-ttl"),
    searchResultLimit                 = config.get[Int]("search.result.limit"),
    searchResultsChunkSize            = config.get[Int]("search.result.chunk-size"),
    searchResultsChunkWindow          = config.get[FiniteDuration]("search.result.chunk-window"),
    searchRequestMaxAllowedIds        = config.get[Int]("search.request.max-allowed-ids"),
    searchRequestMaxAllowedCredIds    = config.get[Int]("search.request.max-allowed-cred-ids"),
    searchRequestMaxAllowedSubjectIds = config.get[Int]("search.request.max-allowed-subject-ids"),
    mongoSearchResultBatchSize        = config.get[Int]("search.result.mongo-batch-size")
  )

  val userAgentAllowList: Seq[String] = config.get[Seq[String]]("user-agent-allow-list")

  val featureToggles: FeatureToggles = FeatureToggles(
    gdsApiV2Enabled        = config.get[Boolean]("feature.gds-api-v2.enabled"),
    v1SearchUseViewEnabled = config.getOptional[Boolean]("feature.v1-search.use-view").getOrElse(false)
  )

  // Test-only event generation
  val testOnlyInsertGroupSize: Int = config.get[Int]("test-only.event-generator.insert-group-size")
  val testOnlyInsertParallelism: Int = config.get[Int]("test-only.event-generator.insert-parallelism")
  val testOnlyViewUpdateGroupSize: Int = config.get[Int]("test-only.event-generator.view-update-group-size")
  val testOnlyViewUpdateParallelism: Int = config.get[Int]("test-only.event-generator.view-update-parallelism")
}

case class EventStoreConfig(searchResultLimit: Int,
                            mongoSearchResultBatchSize: Int,
                            eventTtl: FiniteDuration,
                            searchResultsChunkSize: Int,
                            searchResultsChunkWindow: FiniteDuration,
                            searchRequestMaxAllowedIds: Int,
                            searchRequestMaxAllowedCredIds: Int,
                            searchRequestMaxAllowedSubjectIds: Int
                           )

case class FeatureToggles(
  gdsApiV2Enabled: Boolean,
  v1SearchUseViewEnabled: Boolean
)
