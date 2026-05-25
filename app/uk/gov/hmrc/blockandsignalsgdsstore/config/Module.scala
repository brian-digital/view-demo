/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.config

import com.google.inject.{AbstractModule, Provides}
import uk.gov.hmrc.blockandsignalsgdsstore.controllers.actions.{UserAgentFilter, UserAgentFilterImpl}
import uk.gov.hmrc.blockandsignalsgdsstore.repositories.GDSSearchView

import java.time.Clock
import javax.inject.Singleton
import java.time.Clock

class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[AppConfig]).asEagerSingleton()
    bind(classOf[UserAgentFilter]).to(classOf[UserAgentFilterImpl]).asEagerSingleton()
  }

  @Provides
  def clock(): Clock = Clock.systemUTC()

  @Provides
  @Singleton
  def eventStoreConfig(config: AppConfig): EventStoreConfig = config.eventStoreConfig

  @Provides
  @Singleton
  def featureToggles(config: AppConfig): FeatureToggles = config.featureToggles

}
