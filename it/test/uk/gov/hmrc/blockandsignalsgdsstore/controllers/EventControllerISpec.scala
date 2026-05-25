/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.controllers

import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec
import play.api.Application
import play.api.http.HeaderNames.USER_AGENT
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.libs.ws.*
import play.api.test.Helpers
import play.api.test.Helpers.*
import uk.gov.hmrc.blockandsignalsgdsstore.BaseISpec
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.*
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.SearchResultV2
import uk.gov.hmrc.blockandsignalsgdsstore.repositories.{EventDocumentRepository, GDSSearchView}
import uk.gov.hmrc.blockandsignalsgdsstore.utils.TestData

import java.time.{Clock, Instant, ZoneOffset}

class EventControllerISpec extends BaseISpec with TestData with ScalaFutures {

  private def fixedInstant = Instant.parse("2025-01-25T12:00:00Z")

  override def fakeApplication(): Application = {
    val customClock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    GuiceApplicationBuilder().overrides(bind[Clock].toInstance(customClock)).build()
  }

  val eventDocumentRepository: EventDocumentRepository = app.injector.instanceOf[EventDocumentRepository]

  val gdsSearchView: GDSSearchView = app.injector.instanceOf[GDSSearchView]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(eventDocumentRepository.collection.deleteMany(Filters.exists("_id")).toFuture())
    await(gdsSearchView.deleteEvents())
  }

  "POST /:version/event" should {

    "return 400 Bad Request" when {
      "the :version is not supported" in {
        val response = wsClient
          .url(s"$baseUrl/v3/event")
          .withHttpHeaders(USER_AGENT -> "block-and-signals-frontend")
          .post(Json.obj("some" -> "value"))
          .futureValue
        response.status shouldBe BAD_REQUEST
      }
    }

    "return 403" when {
      "there is no user agent" in {
        val response = wsClient
          .url(s"$baseUrl/anyVersion/event")
          .post(JsNull)
          .futureValue
        response.status shouldBe FORBIDDEN
      }

      "the user agent is not allowed" in {
        val response = wsClient
          .url(s"$baseUrl/anyVersion/event")
          .withHttpHeaders(USER_AGENT -> "invalid user agent")
          .post(JsNull)
          .futureValue
        response.status shouldBe FORBIDDEN
      }
    }
  }

  "POST /v1/event" should {
    def eventRequest(body: JsValue): WSResponse = {
      wsClient
        .url(s"$baseUrl/v1/event")
        .withHttpHeaders(USER_AGENT -> "block-and-signals-frontend")
        .post(body)
        .futureValue
    }

    "return 201 Created" when {
      "an account-concern event is received" in {
        val response = eventRequest(EventRequestV1Json.accountConcernJsonFull)
        val dbResponse: EventDocument = await(eventDocumentRepository.collection.find().head())
        val viewResult: SearchResultV2 = await(gdsSearchView.findFirst()).get

        val submittedOn = Instant.ofEpochMilli(1507644997001L)

        response.status  shouldBe 201
        dbResponse.event shouldBe DatabaseModels.accountConcernEventFull
        dbResponse.event.eventData
          .asInstanceOf[AccountConcernEventData] shouldBe DatabaseModels.accountConcernEventDataFull
        dbResponse.storedAt                      shouldBe fixedInstant
        dbResponse.timeOfInterest                shouldBe submittedOn
        dbResponse.action                        shouldBe None

        viewResult shouldBe SearchResultV2Models.accountConcernFull
      }

      "an account-intervention event is received" in {
        val response = eventRequest(EventRequestV1Json.accountInterventionJsonFull)
        val dbResponse: EventDocument = await(eventDocumentRepository.collection.find().head())
        val viewResult: SearchResultV2 = await(gdsSearchView.findFirst()).get

        response.status  shouldBe 201
        dbResponse.event shouldBe DatabaseModels.accountInterventionEventFull
        dbResponse.event.eventData
          .asInstanceOf[AccountInterventionEventData] shouldBe DatabaseModels.accountInterventionEventDataFull
        dbResponse.storedAt                           shouldBe fixedInstant
        dbResponse.timeOfInterest                     shouldBe Instant.ofEpochMilli(1507644997001L)
        dbResponse.action                             shouldBe Some("re-prove_identity")

        viewResult shouldBe SearchResultV2Models.accountInterventionFull
      }

      "a credential-compromise event is received" in {
        val response = eventRequest(EventRequestV1Json.credentialCompromiseJsonFull)
        val dbResponse: EventDocument = await(eventDocumentRepository.collection.find().head())
        val viewResult: SearchResultV2 = await(gdsSearchView.findFirst()).get

        response.status  shouldBe 201
        dbResponse.event shouldBe DatabaseModels.credentialCompromiseEventFull
        dbResponse.event.eventData
          .asInstanceOf[CredentialCompromiseEventData] shouldBe DatabaseModels.credentialCompromiseEventDataFull
        dbResponse.storedAt                            shouldBe fixedInstant
        dbResponse.timeOfInterest                      shouldBe Instant.ofEpochMilli(1507644997001L)
        dbResponse.action                              shouldBe None

        viewResult shouldBe SearchResultV2Models.credentialCompromiseFull
      }
    }

    "return 400 Bad Request" when {
      "a payload with an unknown intervention code is received" in {
        val response = eventRequest(EventRequestV1Json.credentialCompromiseWrongInterventionCodeJson)

        response.status       shouldBe 400
        response.body[String] shouldBe "Invalid payload: Missing `/details/interventionCode`"
      }

      "a payload with an unknown event type is received" in {
        val response = eventRequest(EventRequestV1Json.unknownEventJson)

        response.status       shouldBe 400
        response.body[String] shouldBe "Invalid payload: Missing `/metadata/signalsEventType`"
      }

      "an invalid payload is received" in {
        val response = eventRequest(Json.parse("""{"invalid": "payload"}"""))

        response.status       shouldBe 400
        response.body[String] shouldBe "Invalid payload: Missing `/metadata`"
      }

      "none of the timestamps fields are present - account concern" in {
        val body =
          s"""
             |{
             |  "metadata": {
             |    "signalsEventType": "accountConcern",
             |    "originalEventType": "ORIGINAL_EVENT_TYPE",
             |    "jti" : "756E69717565206964656E746966696572",
             |    "iat" : 1730392175
             |  },
             |  "details": {
             |    "subjectId": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
             |    "credId": "3434343434343434",
             |    "initiatingEntity": "analyst",
             |    "reason": "account-takeover",
             |    "rationale": "RA99"
             |  }
             |}
             |""".stripMargin

        val response = eventRequest(Json.parse(body))
        response.status       shouldBe 400
        response.body[String] shouldBe "Invalid body: requirement failed: At least one of eventTimestampMs, startTimeMs or endTimeMs must be defined."
      }
    }
  }

  "POST /v2/event" should {
    def eventRequest(body: JsValue): WSResponse = {
      wsClient
        .url(s"$baseUrl/v2/event")
        .withHttpHeaders(USER_AGENT -> "block-and-signals-frontend")
        .post(body)
        .futureValue
    }

    "return 201 Created" when {
      "the event is a Credential Concern" in {
        val response = eventRequest(EventRequestV2Json.credentialConcernJsonFull)
        val dbResponse: EventDocument = await(eventDocumentRepository.collection.find().head())
        val viewResult: SearchResultV2 = await(gdsSearchView.findFirst()).get

        response.status  shouldBe 201
        dbResponse.event shouldBe DatabaseModels.credentialConcernEventFull
        dbResponse.event.eventData
          .asInstanceOf[CredentialConcernEventData] shouldBe DatabaseModels.credentialConcernEventDataFull
        dbResponse.storedAt                         shouldBe fixedInstant
        dbResponse.timeOfInterest                   shouldBe Instant.ofEpochMilli(1507644997001L)
        dbResponse.action                           shouldBe None

        viewResult shouldBe SearchResultV2Models.credentialConcernFull
      }

      "the event is a Device Concern" in {
        val response = eventRequest(EventRequestV2Json.deviceConcernJsonFull)
        val dbResponse: EventDocument = await(eventDocumentRepository.collection.find().head())
        val viewResult: SearchResultV2 = await(gdsSearchView.findFirst()).get

        response.status  shouldBe 201
        dbResponse.event shouldBe DatabaseModels.deviceConcernEventFull
        dbResponse.event.eventData
          .asInstanceOf[DeviceConcernEventData] shouldBe DatabaseModels.deviceConcernEventDataFull
        dbResponse.storedAt                     shouldBe fixedInstant
        dbResponse.timeOfInterest               shouldBe Instant.ofEpochMilli(1507644997001L)
        dbResponse.action                       shouldBe None

        viewResult shouldBe SearchResultV2Models.deviceConcernFull
      }
    }

    "return 400 Bad Request" when {
      "the request is invalid" in {
        val response = eventRequest(Json.obj("invalid" -> "request"))
        response.status     shouldBe 400
        response.body[String] should include("Invalid payload:")
      }
    }
  }
}
