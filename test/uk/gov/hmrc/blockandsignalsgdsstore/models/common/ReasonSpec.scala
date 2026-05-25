/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.common

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class ReasonSpec extends AnyWordSpec with Matchers {

  private val cases = List(
    "user changed password via account management" -> Reason.UserChangedPassword,
    "User aborted session"                         -> Reason.UserAbortedSession,
    "User aborted web session"                     -> Reason.UserAbortedWebSession,
    "Web to app handover failure detected"         -> Reason.WebToAppHandover,
    "An application error occurred"                -> Reason.AppError,
    "Context no longer available after abort"      -> Reason.ContextNoLongerAvailable,
    "The session was aborted while redirecting"    -> Reason.SessionAbortedWhileRedirecting,
    "User aborted face to face session"            -> Reason.UserAbortedF2F,
    "impersonation"                                -> Reason.Impersonation,
    "coercion"                                     -> Reason.Coercion,
    "account-takeover"                             -> Reason.AccTakeover,
    "synthetic-identity"                           -> Reason.SyntheticIdentity,
    "agent-misuse"                                 -> Reason.AgentMisuse,
    "third-party-misuse"                           -> Reason.ThirdPartyMisuse,
    "unspecified"                                  -> Reason.Unspecified,
    "not-set"                                      -> Reason.NotSet,
    "service-misuse"                               -> Reason.ServiceMisuse,
    "stolen"                                       -> Reason.Stolen,
    "lost"                                         -> Reason.Lost,
    "credential-compromise"                        -> Reason.CredentialCompromise,
    "no account found matching this email"         -> Reason.NoAccFound,
    "email value not permitted"                    -> Reason.EmailNotPermitted,
    "email value already in use"                   -> Reason.EmailAlreadyInUse,
    "phishing"                                     -> Reason.Phishing,
    "data-breach"                                  -> Reason.DataBreach,
    "bot-activity"                                 -> Reason.BotActivity
  )

  "Reason" should {
    "read from JSON" in {
      cases.foreach { (input, expected) =>
        Reason.format.reads(JsString(input)) shouldBe JsSuccess(expected)
      }
    }

    "reject invalid JSON" in {
      Reason.format.reads(JsString("asdf")) shouldBe JsError("Unknown Reason: asdf")
      Reason.format.reads(JsNumber(123))    shouldBe JsError("Expected string for Reason")
    }

    "write to JSON" in {
      val caseStrings = cases.map(_.head)
      Reason.values.zip(caseStrings).foreach { (value, stringValue) =>
        Reason.format.writes(value) shouldBe JsString(stringValue)
      }
    }
  }
}
