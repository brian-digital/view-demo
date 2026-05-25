/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.models.common

import play.api.libs.json.*

enum Reason(val stringValue: String):
  case UserChangedPassword            extends Reason("user changed password via account management")
  case UserAbortedSession             extends Reason("User aborted session")
  case UserAbortedWebSession          extends Reason("User aborted web session")
  case WebToAppHandover               extends Reason("Web to app handover failure detected")
  case AppError                       extends Reason("An application error occurred")
  case ContextNoLongerAvailable       extends Reason("Context no longer available after abort")
  case SessionAbortedWhileRedirecting extends Reason("The session was aborted while redirecting")
  case UserAbortedF2F                 extends Reason("User aborted face to face session")
  case Impersonation                  extends Reason("impersonation")
  case Coercion                       extends Reason("coercion")
  case AccTakeover                    extends Reason("account-takeover")
  case SyntheticIdentity              extends Reason("synthetic-identity")
  case AgentMisuse                    extends Reason("agent-misuse")
  case ThirdPartyMisuse               extends Reason("third-party-misuse")
  case Unspecified                    extends Reason("unspecified")
  case NotSet                         extends Reason("not-set")
  case ServiceMisuse                  extends Reason("service-misuse")
  case Stolen                         extends Reason("stolen")
  case Lost                           extends Reason("lost")
  case CredentialCompromise           extends Reason("credential-compromise")
  case NoAccFound                     extends Reason("no account found matching this email")
  case EmailNotPermitted              extends Reason("email value not permitted")
  case EmailAlreadyInUse              extends Reason("email value already in use")
  case Phishing                       extends Reason("phishing")
  case DataBreach                     extends Reason("data-breach")
  case BotActivity                    extends Reason("bot-activity")

object Reason {
  private val reads: Reads[Reason] = Reads {
    case JsString(s) =>
      values.find(_.stringValue == s).map(JsSuccess(_)).getOrElse(JsError(s"Unknown Reason: $s"))
    case _ => JsError("Expected string for Reason")
  }

  private val writes: Writes[Reason] = Writes(ic => JsString(ic.stringValue))

  given format: Format[Reason] = Format(reads, writes)
}
