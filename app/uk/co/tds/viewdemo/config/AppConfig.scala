/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.config

import play.api.Configuration

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration

@Singleton
class AppConfig @Inject() (config: Configuration) {

  val complaintsConfig: ComplaintsConfig = ComplaintsConfig(
    eventTtl                          = config.get[FiniteDuration]("event-ttl"),
    searchResultLimit                 = config.get[Int]("search.result.limit"),
    searchResultsChunkSize            = config.get[Int]("search.result.chunk-size"),
    searchResultsChunkWindow          = config.get[FiniteDuration]("search.result.chunk-window"),
    searchRequestMaxAllowedIds        = config.get[Int]("search.request.max-allowed-ids"),
    searchRequestMaxAllowedCredIds    = config.get[Int]("search.request.max-allowed-cred-ids"),
    searchRequestMaxAllowedSubjectIds = config.get[Int]("search.request.max-allowed-subject-ids"),
    mongoSearchResultBatchSize        = config.get[Int]("search.result.mongo-batch-size")
  )
  val featureToggles: FeatureToggles = FeatureToggles(
    useViewEnabled = config.getOptional[Boolean]("feature.v1-search.use-view").getOrElse(false)
  )
}

case class ComplaintsConfig(searchResultLimit: Int,
                            mongoSearchResultBatchSize: Int,
                            eventTtl: FiniteDuration,
                            searchResultsChunkSize: Int,
                            searchResultsChunkWindow: FiniteDuration,
                            searchRequestMaxAllowedIds: Int,
                            searchRequestMaxAllowedCredIds: Int,
                            searchRequestMaxAllowedSubjectIds: Int
                           )

case class FeatureToggles(
                           useViewEnabled: Boolean
)
