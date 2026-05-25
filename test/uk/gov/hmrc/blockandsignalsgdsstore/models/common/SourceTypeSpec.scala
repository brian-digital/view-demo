/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.models.common

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class SourceTypeSpec extends AnyWordSpec with Matchers {

  private val cases = List(
    "darkweb-special-access-forum"    -> SourceType.DarkWebSpecial,
    "darkweb-data-dump"               -> SourceType.DarkWebData,
    "darkweb-market"                  -> SourceType.DarkWebMarket,
    "darkweb-ransomware-site"         -> SourceType.DarkWebRansom,
    "messaging-platform"              -> SourceType.Messaging,
    "forum"                           -> SourceType.Forum,
    "public-messaging-site"           -> SourceType.PublicMessaging,
    "social-media-site"               -> SourceType.SocialMedia,
    "alternative-social-media-site"   -> SourceType.AltSocialMedia,
    "clear-web-site"                  -> SourceType.Clear,
    "security-breach-disclosure-site" -> SourceType.SecurityBreach,
    "security-vendor-reporting"       -> SourceType.SecurityVendor,
    "rp-system"                       -> SourceType.RpSystem
  )

  "SourceType" should {
    "read from JSON" in {
      cases.foreach { (input, expected) =>
        SourceType.format.reads(JsString(input)) shouldBe JsSuccess(expected)
      }
    }

    "reject invalid JSON" in {
      SourceType.format.reads(JsString("asdf")) shouldBe JsError("Unknown SourceType: asdf")
      SourceType.format.reads(JsNumber(123))    shouldBe JsError("Expected string for SourceType")
    }

    "write to JSON" in {
      val caseStrings = cases.map(_.head)
      SourceType.values.zip(caseStrings).foreach { (value, stringValue) =>
        SourceType.format.writes(value) shouldBe JsString(stringValue)
      }
    }
  }
}
