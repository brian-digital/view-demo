/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.db

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.InitiatingEntity
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.{Event, EventData}
import uk.gov.hmrc.blockandsignalsgdsstore.utils.TestData

class EventSpec extends AnyWordSpec with Matchers with TestData {

  "mongoFormat" should {
    "read valid json" when {
      "it is an account-concern" in {
        val jsResult = Event.mongoFormat.reads(DatabaseModels.JsonData.accountConcernEventJsonFull)
        jsResult     shouldBe a[JsSuccess[Event]]
        jsResult.get shouldBe DatabaseModels.accountConcernEventFull
      }

      "it is an account-intervention" in {
        val jsResult = Event.mongoFormat.reads(DatabaseModels.JsonData.accountInterventionEventJsonFull)
        jsResult     shouldBe a[JsSuccess[Event]]
        jsResult.get shouldBe DatabaseModels.accountInterventionEventFull
      }

      "it is a credential-compromise" in {
        val jsResult = Event.mongoFormat.reads(DatabaseModels.JsonData.credentialCompromiseEventJsonFull)
        jsResult     shouldBe a[JsSuccess[Event]]
        jsResult.get shouldBe DatabaseModels.credentialCompromiseEventFull
      }
    }

    "write the correct json" when {
      "it is an account-concern" in {
        val event = DatabaseModels.accountConcernEventFull
        val expectedJsValue = DatabaseModels.JsonData.accountConcernEventJsonFull
        Json.toJson(event)(Event.mongoFormat) shouldBe expectedJsValue
      }

      "it is an account-intervention" in {
        val event = DatabaseModels.accountInterventionEventFull
        val expectedJsValue = DatabaseModels.JsonData.accountInterventionEventJsonFull
        Json.toJson(event)(Event.mongoFormat) shouldBe expectedJsValue
      }

      "it is a credential-compromise" in {
        val event = DatabaseModels.credentialCompromiseEventFull
        val expectedJsValue = DatabaseModels.JsonData.credentialCompromiseEventJsonFull
        Json.toJson(event)(Event.mongoFormat) shouldBe expectedJsValue
      }
    }
  }
}
