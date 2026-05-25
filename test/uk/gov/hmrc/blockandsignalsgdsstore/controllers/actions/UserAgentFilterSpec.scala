/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.controllers.actions

import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.*
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class UserAgentFilterSpec extends AnyWordSpec with Matchers {

  private val app: Application = new GuiceApplicationBuilder()
    .configure(
      "user-agent-allow-list.0" -> "allowed-user-agent-one",
      "user-agent-allow-list.1" -> "allowed-user-agent-two"
    )
    .build()

  private implicit lazy val materializer: Materializer = app.materializer
  private implicit lazy val Action: DefaultActionBuilder = app.injector.instanceOf(classOf[DefaultActionBuilder])

  private val userAgentFilter = app.injector.instanceOf[UserAgentFilter]

  private val testAction: EssentialAction = (Action andThen userAgentFilter)(Ok("success"))

  "UserAgentFilter" should {
    "block requests without a user agent" in {
      val request = FakeRequest(GET, "/")
      val result = call(testAction, request)
      status(result) shouldBe FORBIDDEN
    }

    "block requests from user agents that are not allowed" in {
      val request = FakeRequest(GET, "/").withHeaders(USER_AGENT -> "blocked-user-agent")
      val result = call(testAction, request)
      status(result) shouldBe FORBIDDEN
    }

    "allow requests from user agents that are allowed" in {
      val request = FakeRequest(GET, "/").withHeaders(USER_AGENT -> "allowed-user-agent-one")
      val result = call(testAction, request)
      status(result) shouldBe OK

      val request2 = FakeRequest(GET, "/").withHeaders(USER_AGENT -> "allowed-user-agent-two")
      val result2 = call(testAction, request)
      status(result2) shouldBe OK
    }
  }

}
