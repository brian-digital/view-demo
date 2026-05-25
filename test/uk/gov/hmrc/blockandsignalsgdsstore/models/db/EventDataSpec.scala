/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.db

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*
import uk.gov.hmrc.blockandsignalsgdsstore.utils.TestData

class EventDataSpec extends AnyWordSpec with Matchers with TestData {

  "mongoReads()" should {
    "provide a working Reads[EventData]" when {
      "the event type is EventType.AccountConcern" in {
        val reads: Reads[EventData] = EventData.mongoReads(EventType.AccountConcern)
        val jsResult: JsResult[EventData] = reads.reads(DatabaseModels.JsonData.accountConcernEventDataJsonFull)
        jsResult     shouldBe a[JsSuccess[AccountConcernEventData]]
        jsResult.get shouldBe DatabaseModels.accountConcernEventDataFull
      }

      "the event type is EventType.AccountIntervention" in {
        val reads: Reads[EventData] = EventData.mongoReads(EventType.AccountIntervention)
        val jsResult: JsResult[EventData] = reads.reads(DatabaseModels.JsonData.accountInterventionEventDataJsonFull)
        jsResult     shouldBe a[JsSuccess[AccountInterventionEventData]]
        jsResult.get shouldBe DatabaseModels.accountInterventionEventDataFull
      }

      "the event type is EventType.CredentialCompromise" in {
        val reads: Reads[EventData] = EventData.mongoReads(EventType.CredentialCompromise)
        val jsResult: JsResult[EventData] = reads.reads(DatabaseModels.JsonData.credentialCompromiseEventDataJsonFull)
        jsResult     shouldBe a[JsSuccess[CredentialCompromiseEventData]]
        jsResult.get shouldBe DatabaseModels.credentialCompromiseEventDataFull
      }
    }
  }

  "mongoWrites" should {
    "generate JSON" when {
      "it is an account-concern event" in {
        val model = DatabaseModels.accountConcernEventDataFull
        val expected = DatabaseModels.JsonData.accountConcernEventDataJsonFull
        Json.toJson(model)(EventData.mongoWrites) shouldBe expected
      }

      "it is an account-intervention event" in {
        val model = DatabaseModels.accountInterventionEventDataFull
        val expected = DatabaseModels.JsonData.accountInterventionEventDataJsonFull
        Json.toJson(model)(EventData.mongoWrites) shouldBe expected
      }

      "it is an credential-compromise event" in {
        val model = DatabaseModels.credentialCompromiseEventDataFull
        val expected = DatabaseModels.JsonData.credentialCompromiseEventDataJsonFull
        Json.toJson(model)(EventData.mongoWrites) shouldBe expected
      }
    }
  }

}
