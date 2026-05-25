/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.config

import com.google.inject.{AbstractModule, Provides}
import uk.co.tds.viewdemo.repositories.SearchView

import java.time.Clock
import javax.inject.Singleton
import java.time.Clock

class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[AppConfig]).asEagerSingleton()
  }

  @Provides
  def clock(): Clock = Clock.systemUTC()

  @Provides
  @Singleton
  def complaintsConfig(config: AppConfig): ComplaintsConfig = config.complaintsConfig

  @Provides
  @Singleton
  def featureToggles(config: AppConfig): FeatureToggles = config.featureToggles

}
