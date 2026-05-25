/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.models.common

import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Reads, Writes}

enum SourceType(val stringValue: String):
  case DarkWebSpecial  extends SourceType("darkweb-special-access-forum")
  case DarkWebData     extends SourceType("darkweb-data-dump")
  case DarkWebMarket   extends SourceType("darkweb-market")
  case DarkWebRansom   extends SourceType("darkweb-ransomware-site")
  case Messaging       extends SourceType("messaging-platform")
  case Forum           extends SourceType("forum")
  case PublicMessaging extends SourceType("public-messaging-site")
  case SocialMedia     extends SourceType("social-media-site")
  case AltSocialMedia  extends SourceType("alternative-social-media-site")
  case Clear           extends SourceType("clear-web-site")
  case SecurityBreach  extends SourceType("security-breach-disclosure-site")
  case SecurityVendor  extends SourceType("security-vendor-reporting")
  case RpSystem        extends SourceType("rp-system")

object SourceType {
  private val reads: Reads[SourceType] = Reads {
    case JsString(s) =>
      values.find(_.stringValue == s).map(JsSuccess(_)).getOrElse(JsError(s"Unknown SourceType: $s"))
    case _ => JsError("Expected string for SourceType")
  }

  private val writes: Writes[SourceType] = Writes(ic => JsString(ic.stringValue))

  given format: Format[SourceType] = Format(reads, writes)
}
