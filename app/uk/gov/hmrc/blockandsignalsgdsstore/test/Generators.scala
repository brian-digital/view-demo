/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.test

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.{Flow, Source}
import org.scalacheck.Gen
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.*
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.*
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.{SearchRequest, SearchResult, SearchResultV2}
import uk.gov.hmrc.blockandsignalsgdsstore.utils.StreamUtils

import java.time.{Instant, LocalDate, ZoneId}

/** Test data generators - put in main code so testOnly data setup can use generators to create test data
  */
trait Generators {

  private val nonEmptyStringGen: Gen[String] = for {
    length <- Gen.chooseNum(1, 50)
    chars  <- Gen.listOfN(length, Gen.alphaChar)
  } yield chars.mkString

  val credIdGen: Gen[String] = Gen.chooseNum(1, 5_000_000).map(n => "%016d".format(n))

  val subjectIdGen: Gen[String] = for {
    length <- Gen.chooseNum(39, 50)
    chars  <- Gen.listOfN(length, Gen.alphaNumChar)
  } yield s"urn:fdc:gov.uk:2022:${chars.mkString}"

  // nb if the "test-" prefix is changed then the
  // uk.gov.hmrc.blockandsignalsgdsstore.repositories.EventDocumentRepository.deleteTestEvents
  // will need to change in tandem so that deleting test events remains possible
  private val eventIdGen: Gen[String] = Gen.uuid.map(i => s"test-$i")

  private val localDateGen: Gen[LocalDate] =
    Gen.calendar.map(c => Instant.ofEpochMilli(c.getTimeInMillis).atZone(ZoneId.of("UTC")).toLocalDate)

  private val instantGen: Gen[Instant] = Gen.calendar.map(c => Instant.ofEpochMilli(c.getTimeInMillis))

  private def instantGen(from: Instant, to: Instant): Gen[Instant] =
    Gen.chooseNum(from.toEpochMilli, to.toEpochMilli).map(Instant.ofEpochMilli)

  private val stateGen: Gen[String] = Gen.oneOf("active", "suspended", "permanently_suspended")

  private val eventTypeNotAccountInterventionGen: Gen[EventType] =
    Gen.oneOf(EventType.AccountConcern, EventType.CredentialCompromise)

  private val actionGen: Gen[String] = Gen.oneOf("suspended", "permanently_suspended", "reset_password", "re-prove_identity", "reset_password_and_re-prove_identity")

  private val reasonGen: Gen[String] = Gen.oneOf(
    "user changed password via account management",
    "User aborted session",
    "User aborted web session",
    "Web to app handover failure detected",
    "An application error occurred",
    "Context no longer available after abort",
    "The session was aborted while redirecting",
    "User aborted face to face session",
    "impersonation",
    "coercion",
    "account-takeover",
    "synthetic-identity",
    "agent-misuse",
    "third-party-misuse",
    "unspecified",
    "not-set",
    "service-misuse",
    "stolen",
    "lost",
    "credential-compromise",
    "no account found matching this email",
    "email value not permitted",
    "email value already in use"
  )

  private val rationaleGen: Gen[String] = Gen.oneOf("RA01", "RA02", "RA03", "RA04", "RA05", "RA06", "RA07", "RA99")

  private val reasonEnumGen: Gen[Reason] = Gen.oneOf(Reason.values.toSeq)

  private val rationaleEnumGen: Gen[Rationale] = Gen.oneOf(Rationale.values.toSeq)

  private val initiatingEntityGen: Gen[InitiatingEntity] =
    Gen.oneOf(InitiatingEntity.Admin, InitiatingEntity.User, InitiatingEntity.Policy, InitiatingEntity.System)

  private val initiatingEntityAnalyst: InitiatingEntity = InitiatingEntity.Analyst

  private val credentialTypeStringGen: Gen[String] = Gen.oneOf("CredType1", "CredType2", "CredType3")

  private val credentialTypeGen: Gen[CredentialType] =
    Gen.oneOf(CredentialType.Account, CredentialType.IR, CredentialType.CR)

  private val reasonAdminGen: Gen[String] = Gen.oneOf("Admin 1", "Admin 2", "Admin 3")

  private val reasonUserGen: Gen[String] = Gen.oneOf("User 1", "User 2", "User 3")

  private val emailAddressGen: Gen[String] =
    Gen.oneOf("BarrySmith@email.com", "JaneSmith@email.co.uk", "ThomasSmith@eamil.com")

  private val interventionCodeGen: Gen[InterventionCode] = Gen.oneOf(InterventionCode.values)

  val validSearchRequestGen: Gen[SearchRequest] = for {
    dateFrom <- localDateGen
    dateTo = dateFrom.plusDays(1)
    credIds    <- Gen.option(Gen.listOfN(10, credIdGen))
    subjectIds <- Gen.option(Gen.listOfN(10, subjectIdGen))
    eventType  <- Gen.some(eventTypeNotAccountInterventionGen)
  } yield {
    SearchRequest(dateFrom = dateFrom, dateTo = dateTo, credIds = credIds, subjectIds = subjectIds, eventType = eventType)
  }

  val searchResultV2Gen: Gen[SearchResultV2] = for {
    eventType                 <- eventTypeNotAccountInterventionGen
    hmrcCredentialId          <- Gen.option(credIdGen)
    oneLoginSubjectId         <- Gen.option(subjectIdGen)
    submittedOn               <- instantGen
    submittedBy               <- Gen.option(initiatingEntityGen)
    reason                    <- Gen.option(reasonGen)
    rationale                 <- Gen.option(rationaleGen)
    accountInterventionState  <- Gen.option(stateGen)
    accountInterventionAction <- Gen.option(actionGen)
    credentialCompromiseEmailAddress = None
    credentialCompromiseInterventionCode = None
    credentialConcernSourceType = None
    credentialConcernSourceUri = None
    credentialConcernCredentialType = None
    credentialConcernIdentifierFormat = None
    credentialConcernDocumentNumber = None
    credentialConcernExpiryDate = None
    credentialConcernIcaoIssuerCode = None
    credentialConcernPersonalNumber = None
    credentialConcernIssueNumber = None
    credentialConcernIssuedBy = None
    credentialConcernIssuingCountry = None
    credentialConcernEmailAddress = None
    credentialConcernTelephoneNumber = None
    credentialConcernNino = None
    deviceConcernSourceType = None
    deviceConcernSourceUri = None
    deviceConcernDeviceHash = None
    deviceCookieJourneyId = None
    deviceConcernPersistentSessionCookie = None
    deviceConcernDeviceId = None
    deviceConcernSessionId = None
    deviceConcernIpAddress = None
  } yield {
    SearchResultV2(
      eventType                            = eventType,
      hmrcCredentialId                     = hmrcCredentialId,
      oneLoginSubjectId                    = oneLoginSubjectId,
      submittedOn                          = submittedOn,
      submittedBy                          = submittedBy,
      reason                               = reason,
      rationale                            = rationale,
      accountInterventionState             = accountInterventionState,
      accountInterventionAction            = accountInterventionAction,
      credentialCompromiseEmailAddress     = credentialCompromiseEmailAddress,
      credentialCompromiseInterventionCode = credentialCompromiseInterventionCode,
      credentialConcernSourceType          = credentialConcernSourceType,
      credentialConcernSourceUri           = credentialConcernSourceUri,
      credentialConcernCredentialType      = credentialConcernCredentialType,
      credentialConcernIdentifierFormat    = credentialConcernIdentifierFormat,
      credentialConcernDocumentNumber      = credentialConcernDocumentNumber,
      credentialConcernExpiryDate          = credentialConcernExpiryDate,
      credentialConcernIcaoIssuerCode      = credentialConcernIcaoIssuerCode,
      credentialConcernPersonalNumber      = credentialConcernPersonalNumber,
      credentialConcernIssueNumber         = credentialConcernIssueNumber,
      credentialConcernIssuedBy            = credentialConcernIssuedBy,
      credentialConcernIssuingCountry      = credentialConcernIssuingCountry,
      credentialConcernEmailAddress        = credentialConcernEmailAddress,
      credentialConcernTelephoneNumber     = credentialConcernTelephoneNumber,
      credentialConcernNino                = credentialConcernNino,
      deviceConcernSourceType              = deviceConcernSourceType,
      deviceConcernSourceUri               = deviceConcernSourceUri,
      deviceConcernDeviceHash              = deviceConcernDeviceHash,
      deviceCookieJourneyId                = deviceCookieJourneyId,
      deviceConcernPersistentSessionCookie = deviceConcernPersistentSessionCookie,
      deviceConcernDeviceId                = deviceConcernDeviceId,
      deviceConcernSessionId               = deviceConcernSessionId,
      deviceConcernIpAddress               = deviceConcernIpAddress
    )
  }

  val searchResultsV2Gen: Gen[Seq[SearchResultV2]] = for {
    num     <- Gen.chooseNum(1, 20)
    results <- Gen.listOfN(num, searchResultV2Gen)
  } yield results

  val searchResultGen: Gen[SearchResult] = for {
    credId           <- credIdGen
    subjectId        <- subjectIdGen
    timeOfInterest   <- instantGen
    eventType        <- eventTypeNotAccountInterventionGen
    action           <- Gen.option(actionGen)
    reason           <- Gen.option(reasonGen)
    rationale        <- Gen.option(rationaleGen)
    initiatingEntity <- Gen.option(initiatingEntityGen)
    credentialType   <- Gen.option(credentialTypeGen)
    reasonAdmin      <- Gen.option(reasonAdminGen)
    reasonUser       <- Gen.option(reasonUserGen)
    emailAddress     <- Gen.option(emailAddressGen)
    interventionCode <- Gen.option(interventionCodeGen)
  } yield {
    SearchResult(
      credId           = Some(credId),
      subjectId        = Some(subjectId),
      timeOfInterest   = timeOfInterest,
      eventType        = eventType,
      action           = action,
      reason           = reason,
      rationale        = rationale,
      initiatingEntity = initiatingEntity,
      credentialType   = credentialType,
      reasonAdmin      = reasonAdmin,
      reasonUser       = reasonUser,
      emailAddress     = emailAddress,
      interventionCode = interventionCode
    )
  }

  val searchResultsGen: Gen[Seq[SearchResult]] = for {
    num     <- Gen.chooseNum(1, 20)
    results <- Gen.listOfN(num, searchResultGen)
  } yield results

  private val accountConcernEventDataTypeGen: Gen[(EventType, AccountConcernEventData)] = for {
    initiatingEntity <- Gen.option(initiatingEntityGen)
    reason           <- Gen.option(reasonGen)
    rationale        <- Gen.option(rationaleGen)
    eventTimestampMs <-
      instantGen.map(
        _.toEpochMilli
      ) // at least one of the timestamps must be present, we make this one present all the time for valid testing purposes
    startTimeMs <- Gen.option(instantGen.map(_.toEpochMilli))
    endTimeMs   <- Gen.option(instantGen.map(_.toEpochMilli))
    eventType   <- Gen.const(EventType.AccountConcern)
  } yield {
    val eventData = AccountConcernEventData(
      initiatingEntity = initiatingEntity,
      reason           = reason,
      rationale        = rationale,
      eventTimestampMs = Some(
        eventTimestampMs
      ), // at least one of the timestamps must be present, we make this one present all the time for valid testing purposes
      startTimeMs = startTimeMs,
      endTimeMs   = endTimeMs
    )
    (eventType, eventData)
  }

  private def accountConcernEventDataTypeGen(ts: Instant): Gen[(EventType, EventData)] = for {
    concern <- accountConcernEventDataTypeGen.map { (eType, data) =>
                 val updated = data.copy(eventTimestampMs = Some(ts.toEpochMilli), startTimeMs = Some(ts.toEpochMilli), endTimeMs = Some(ts.toEpochMilli))
                 (eType, updated)
               }

  } yield concern

  private def accountInterventionEventDataTypeGen(isAnalyst: Boolean): Gen[(EventType, AccountInterventionEventData)] =
    for {
      initiatingEntity <- if (isAnalyst) Gen.some(initiatingEntityAnalyst) else Gen.option(initiatingEntityGen)
      state            <- Gen.option(stateGen)
      action           <- Gen.option(actionGen)
      eventTimestampMs <- instantGen.map(_.toEpochMilli)
      eventType        <- Gen.const(EventType.AccountIntervention)
    } yield {
      val eventData = AccountInterventionEventData(
        initiatingEntity = initiatingEntity,
        state            = state,
        action           = action,
        eventTimestampMs = eventTimestampMs
      )
      (eventType, eventData)
    }

  private def accountInterventionEventDataTypeGen(ts: Instant, isAnalyst: Boolean = false): Gen[(EventType, EventData)] =
    for {
      intervention <- accountInterventionEventDataTypeGen(isAnalyst).map { (eType, data) =>
                        val updated = data.copy(eventTimestampMs = ts.toEpochMilli)
                        (eType, updated)
                      }
    } yield intervention

  private val credentialCompromiseEventDataTypeGen: Gen[(EventType, CredentialCompromiseEventData)] = for {
    initiatingEntity <- Gen.option(initiatingEntityGen)
    credentialType   <- Gen.option(credentialTypeGen)
    eventTimestampMs <- instantGen.map(_.toEpochMilli)
    reasonAdmin      <- Gen.option(reasonAdminGen)
    reasonUser       <- Gen.option(reasonUserGen)
    emailAddress     <- Gen.option(emailAddressGen)
    interventionCode <- Gen.option(interventionCodeGen)
    eventType        <- Gen.const(EventType.CredentialCompromise)
  } yield {
    val eventData = CredentialCompromiseEventData(
      initiatingEntity = initiatingEntity,
      credentialType   = credentialType,
      eventTimestampMs = eventTimestampMs,
      reasonAdmin      = reasonAdmin,
      reasonUser       = reasonUser,
      emailAddress     = emailAddress,
      interventionCode = interventionCode
    )
    (eventType, eventData)
  }

  private def credentialCompromiseEventDataTypeGen(ts: Instant): Gen[(EventType, EventData)] = for {
    credentialCompromise <- credentialCompromiseEventDataTypeGen.map { (eType, data) =>
                              val updated = data.copy(eventTimestampMs = ts.toEpochMilli)
                              (eType, updated)
                            }
  } yield credentialCompromise

  private def deviceConcernEventDataTypeGen: Gen[(EventType, DeviceConcernEventData)] =
    for {
      initiatingEntity <- initiatingEntityGen
      reasonAdmin      <- reasonEnumGen
      rationale        <- rationaleEnumGen
      eventTimestampMs <- instantGen.map(_.toEpochMilli)
      startTimeMs      <- Gen.option(instantGen.map(_.toEpochMilli))
      endTimeMs        <- Gen.option(instantGen.map(_.toEpochMilli))
      iss              <- Gen.option(nonEmptyStringGen)
      sub              <- Gen.option(nonEmptyStringGen)
      sourceType       <- Gen.option(Gen.oneOf(SourceType.values.toSeq))
      sourceTypeUri    <- Gen.option(nonEmptyStringGen)
      eventType        <- Gen.const(EventType.DeviceConcern)
    } yield {
      val eventData = DeviceConcernEventData(
        initiatingEntity = initiatingEntity,
        reasonAdmin      = reasonAdmin,
        rationale        = rationale,
        eventTimestampMs = Some(eventTimestampMs),
        startTimeMs      = startTimeMs,
        endTimeMs        = endTimeMs,
        iss              = iss,
        sub              = sub,
        identifiers      = List(),
        sourceType       = sourceType,
        sourceTypeUri    = sourceTypeUri
      )
      (eventType, eventData)
    }

  private def deviceConcernEventDataTypeGen(ts: Instant): Gen[(EventType, EventData)] =
    for {
      deviceConcern <- deviceConcernEventDataTypeGen.map { (eType, data) =>
                         val updated = data.copy(eventTimestampMs = Some(ts.toEpochMilli), startTimeMs = Some(ts.toEpochMilli), endTimeMs = Some(ts.toEpochMilli))
                         (eType, updated)
                       }
    } yield deviceConcern

  private def credentialConcernEventDataTypeGen: Gen[(EventType, CredentialConcernEventData)] =
    for {
      credentialType   <- Gen.option(credentialTypeGen)
      initiatingEntity <- initiatingEntityGen
      reasonAdmin      <- reasonEnumGen
      rationale        <- rationaleEnumGen
      eventTimestampMs <- instantGen.map(_.toEpochMilli)
      startTimeMs      <- Gen.option(instantGen.map(_.toEpochMilli))
      endTimeMs        <- Gen.option(instantGen.map(_.toEpochMilli))
      identifierFormat <- nonEmptyStringGen
      documentNumber   <- Gen.option(nonEmptyStringGen)
      expiryDate       <- Gen.option(nonEmptyStringGen)
      icaoIssuerCode   <- Gen.option(nonEmptyStringGen)
      personalNumber   <- Gen.option(nonEmptyStringGen)
      issueNumber      <- Gen.option(nonEmptyStringGen)
      issuedBy         <- Gen.option(nonEmptyStringGen)
      email            <- Gen.option(emailAddressGen)
      phoneNumber      <- Gen.option(nonEmptyStringGen)
      nino             <- Gen.option(nonEmptyStringGen)
      sourceType       <- Gen.option(Gen.oneOf(SourceType.values.toSeq))
      sourceTypeUri    <- Gen.option(nonEmptyStringGen)
      eventType        <- Gen.const(EventType.CredentialConcern)
    } yield {
      val eventData = CredentialConcernEventData(
        credentialType   = credentialType,
        initiatingEntity = initiatingEntity,
        reasonAdmin      = reasonAdmin,
        rationale        = rationale,
        eventTimestampMs = Some(eventTimestampMs),
        startTimeMs      = startTimeMs,
        endTimeMs        = endTimeMs,
        identifierFormat = identifierFormat,
        documentNumber   = documentNumber,
        expiryDate       = expiryDate,
        icaoIssuerCode   = icaoIssuerCode,
        personalNumber   = personalNumber,
        issueNumber      = issueNumber,
        issuedBy         = issuedBy,
        email            = email,
        phoneNumber      = phoneNumber,
        nino             = nino,
        sourceType       = sourceType,
        sourceTypeUri    = sourceTypeUri
      )
      (eventType, eventData)
    }

  private def credentialConcernEventDataTypeGen(ts: Instant): Gen[(EventType, EventData)] =
    for {
      credentialConcern <- credentialConcernEventDataTypeGen.map { (eType, data) =>
                             val updated = data.copy(eventTimestampMs = Some(ts.toEpochMilli), startTimeMs = Some(ts.toEpochMilli), endTimeMs = Some(ts.toEpochMilli))
                             (eType, updated)
                           }
    } yield credentialConcern

  val eventGen: Gen[Event] = for {
    (eventType, eventData) <-
      Gen.oneOf(
        accountInterventionEventDataTypeGen(false),
        accountConcernEventDataTypeGen,
        credentialCompromiseEventDataTypeGen,
        deviceConcernEventDataTypeGen,
        credentialConcernEventDataTypeGen
      )
    originalEventType <- Gen.const(eventType.mongoValue)
    eventId           <- eventIdGen
    generatedAt       <- instantGen.map(_.toEpochMilli)
    subjectId         <- subjectIdGen
    credId            <- credIdGen
  } yield {
    Event(
      eventType         = eventType,
      originalEventType = originalEventType,
      eventId           = eventId,
      generatedAt       = generatedAt,
      subjectId         = Some(subjectId),
      credId            = Some(credId),
      eventData         = eventData
    )
  }

  def eventGen(from: Instant, to: Instant): Gen[Event] = for {
    ts                     <- instantGen(from, to)
    (eventType, eventData) <- Gen.oneOf(accountInterventionEventDataTypeGen(ts), accountConcernEventDataTypeGen(ts))
    event                  <- eventGen.map(_.copy(eventType = eventType, eventData = eventData, generatedAt = ts.toEpochMilli))
  } yield event

  private def concernEventDataTypeGen(from: Instant, to: Instant): Gen[Event] = for {
    ts <- instantGen(from, to)
    (eventType, eventData) <- Gen.oneOf(
                                accountConcernEventDataTypeGen(ts),
                                deviceConcernEventDataTypeGen(ts),
                                credentialConcernEventDataTypeGen(ts),
                                credentialCompromiseEventDataTypeGen(ts),
                                accountInterventionEventDataTypeGen(ts, isAnalyst = true)
                              )
    event <- eventGen.map(_.copy(eventType = eventType, eventData = eventData, generatedAt = ts.toEpochMilli))
  } yield event

  private def eventsForCredsSubjectGen(from: Instant, to: Instant, maxEventCountPerIdPair: Int): Gen[Seq[Event]] = for {
    numEvents <- Gen.chooseNum(1, maxEventCountPerIdPair)
    credId    <- credIdGen
    subjectId <- subjectIdGen
    es        <- Gen.listOfN(numEvents, concernEventDataTypeGen(from, to).map(_.copy(credId = Some(credId), subjectId = Some(subjectId))))
  } yield es

  def eventGenV1Only(from: Instant, to: Instant): Gen[Event] = for {
    ts                     <- instantGen(from, to)
    (eventType, eventData) <- Gen.oneOf(accountInterventionEventDataTypeGen(ts, isAnalyst = true), accountConcernEventDataTypeGen(ts), credentialCompromiseEventDataTypeGen(ts))
    event                  <- eventGen.map(_.copy(eventType = eventType, eventData = eventData, generatedAt = ts.toEpochMilli))
  } yield event

  // Generate events one-by-one using a Pekko Stream
  // This avoids loading all events into memory when using the test event creation endpoint
  def eventGeneratorStream(totalEventCount: Int, eventsPerAccountId: Int, from: Instant, to: Instant, randomlyGenerate: Boolean, v2EventsAvailable: Boolean): Source[Event, NotUsed] = {

    def randomAccountIdGenerator(): (String, String) = (credIdGen.sample.get, subjectIdGen.sample.get)

    def sequentialAccountIdGenerator(index: Int): (String, String) = {
      val credId: String = "0" * (16 - index.toString.length) + index
      val subjectId: String = subjectIdGen.sample.get
      (credId, subjectId)
    }

    val accountIdGenerator: Int => (String, String) =
      if randomlyGenerate
      then _ => randomAccountIdGenerator()
      else index => sequentialAccountIdGenerator(index)

    def v1Events(credId: String, subjectId: String): List[Event] =
      List.fill(eventsPerAccountId) {
        eventGenV1Only(from, to).sample.get.copy(credId = Some(credId), subjectId = Some(subjectId))
      }

    def v1AndV2Events(credId: String, subjectId: String): List[Event] =
      List.fill(eventsPerAccountId) {
        concernEventDataTypeGen(from, to).sample.get.copy(credId = Some(credId), subjectId = Some(subjectId))
      }

    val eventGenerator: Flow[(String, String), List[Event], NotUsed] = {
      val eventListF: (String, String) => List[Event] = if v2EventsAvailable then v1AndV2Events else v1Events
      Flow[(String, String)].map { (credId, subjectId) =>
        eventListF(credId, subjectId)
      }
    }

    val nAccounts: Int = totalEventCount / eventsPerAccountId
    Source(1 to nAccounts)
      .map(index => accountIdGenerator(index))
      .via(eventGenerator)
      .via(StreamUtils.ungrouper)
  }

  def eventsBetweenGen(from: Instant, to: Instant): Gen[Seq[Event]] = for {
    length <- Gen.chooseNum(1, 50)
    events <- Gen.listOfN(length, eventGen(from, to))
  } yield events

  def eventsConcernEventBetweenGen(from: Instant, to: Instant): Gen[Seq[Event]] = for {
    length <- Gen.chooseNum(1, 50)
    events <- Gen.listOfN(length, concernEventDataTypeGen(from, to))
  } yield events

  def interventionEventGen(isAnalyst: Boolean = false): Gen[Event] = for {
    (eventType, eventData) <- accountInterventionEventDataTypeGen(isAnalyst)
    event                  <- eventGen.map(_.copy(eventType = eventType, eventData = eventData))
  } yield event

  def interventionEventGen(from: Instant, to: Instant, isAnalyst: Boolean): Gen[Event] = for {
    ts                     <- instantGen(from, to)
    (eventType, eventData) <- accountInterventionEventDataTypeGen(ts, isAnalyst)
    event                  <- eventGen.map(_.copy(eventType = eventType, eventData = eventData, generatedAt = ts.toEpochMilli))
  } yield event

  def concernEventGen(from: Instant, to: Instant): Gen[Event] = for {
    ts                     <- instantGen(from, to)
    (eventType, eventData) <- accountConcernEventDataTypeGen(ts)
    event                  <- eventGen.map(_.copy(eventType = eventType, eventData = eventData, generatedAt = ts.toEpochMilli))
  } yield event

  def compromiseEventGen(from: Instant, to: Instant): Gen[Event] = for {
    ts                     <- instantGen(from, to)
    (eventType, eventData) <- credentialCompromiseEventDataTypeGen(ts)
    event                  <- eventGen.map(_.copy(eventType = eventType, eventData = eventData, generatedAt = ts.toEpochMilli))
  } yield event

  def credentialConcernEventGen(from: Instant, to: Instant): Gen[Event] = for {
    ts                     <- instantGen(from, to)
    (eventType, eventData) <- credentialConcernEventDataTypeGen(ts)
    event                  <- eventGen.map(_.copy(eventType = eventType, eventData = eventData, generatedAt = ts.toEpochMilli))
  } yield event

  def deviceConcernEventGen(from: Instant, to: Instant): Gen[Event] = for {
    ts                     <- instantGen(from, to)
    (eventType, eventData) <- deviceConcernEventDataTypeGen(ts)
    event                  <- eventGen.map(_.copy(eventType = eventType, eventData = eventData, generatedAt = ts.toEpochMilli))
  } yield event
}

object Generators extends Generators
