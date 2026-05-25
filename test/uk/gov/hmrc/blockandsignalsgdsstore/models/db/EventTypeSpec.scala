/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.db

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.EventType

class EventTypeSpec extends AnyWordSpec with Matchers {

  "searchValueReads" should {
    "read json to EventType model" in {
      val concernEventType: JsResult[EventType] =
        Json.fromJson[EventType](JsString("account-concern"))(EventType.searchValueReads)
      concernEventType.get shouldBe EventType.AccountConcern

      val interventionEventType: JsResult[EventType] =
        Json.fromJson[EventType](JsString("account-intervention"))(EventType.searchValueReads)
      interventionEventType.get shouldBe EventType.AccountIntervention

      val credentialCompromiseEventType: JsResult[EventType] =
        Json.fromJson[EventType](JsString("credential-compromise"))(EventType.searchValueReads)
      credentialCompromiseEventType.get shouldBe EventType.CredentialCompromise

      val unknownEventType: JsResult[EventType] =
        Json.fromJson[EventType](JsString("foobar"))(EventType.searchValueReads)
      unknownEventType shouldBe JsError("Invalid eventType: foobar")
    }
  }

  "searchValueWrites" should {
    "write EventType model to json" in {
      val concernEventType: JsValue = Json.toJson[EventType](EventType.AccountConcern)(EventType.searchValueWrites)
      concernEventType shouldBe JsString("account-concern")

      val interventionEventType: JsValue =
        Json.toJson[EventType](EventType.AccountIntervention)(EventType.searchValueWrites)
      interventionEventType shouldBe JsString("account-intervention")

      val credentialCompromiseEventType: JsValue =
        Json.toJson[EventType](EventType.CredentialCompromise)(EventType.searchValueWrites)
      credentialCompromiseEventType shouldBe JsString("credential-compromise")
    }
  }

  "mongoFormat" should {
    "read json to EventType model" in {
      val concernEventType: JsResult[EventType] =
        Json.fromJson[EventType](JsString("accountConcern"))(EventType.mongoFormat)
      concernEventType.get shouldBe EventType.AccountConcern

      val interventionEventType: JsResult[EventType] =
        Json.fromJson[EventType](JsString("accountIntervention"))(EventType.mongoFormat)
      interventionEventType.get shouldBe EventType.AccountIntervention

      val credentialCompromiseEventType: JsResult[EventType] =
        Json.fromJson[EventType](JsString("credentialCompromise"))(EventType.mongoFormat)
      credentialCompromiseEventType.get shouldBe EventType.CredentialCompromise

      val unknownEventType: JsResult[EventType] = Json.fromJson[EventType](JsString("foobar"))(EventType.mongoFormat)
      unknownEventType shouldBe JsError("Invalid eventType: foobar")
    }

    "write EventType model to json" in {
      val concernEventType: JsValue = Json.toJson[EventType](EventType.AccountConcern)(EventType.mongoFormat)
      concernEventType shouldBe JsString("accountConcern")

      val interventionEventType: JsValue = Json.toJson[EventType](EventType.AccountIntervention)(EventType.mongoFormat)
      interventionEventType shouldBe JsString("accountIntervention")

      val credentialCompromiseEventType: JsValue =
        Json.toJson[EventType](EventType.CredentialCompromise)(EventType.mongoFormat)
      credentialCompromiseEventType shouldBe JsString("credentialCompromise")
    }
  }
}
