/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.test

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Attributes
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import org.bson.types.ObjectId
import play.api.Logging
import uk.gov.hmrc.blockandsignalsgdsstore.adapters.SignalProcessorEventAdapters
import uk.gov.hmrc.blockandsignalsgdsstore.config.AppConfig
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.{Event, EventDocument}
import uk.gov.hmrc.blockandsignalsgdsstore.repositories.{EventDocumentRepository, GDSSearchView}
import uk.gov.hmrc.blockandsignalsgdsstore.utils.StreamUtils

import java.time.{Clock, Duration, Instant, LocalDate}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class TestEventGeneratorService @Inject() (
  appConfig: AppConfig,
  eventDocumentRepository: EventDocumentRepository,
  clock: Clock,
  gdsSearchView: GDSSearchView
)(implicit
  ec: ExecutionContext,
  actorSystem: ActorSystem
) extends Logging {

  def deleteTestEvents(): Future[Unit] = eventDocumentRepository.deleteEvents().map(_ => ())

  def clearDBSettingTTL(): Future[Unit] = eventDocumentRepository.clearDBUsingTTL().map(_ => ())

  def clearViewSettingTTL(): Future[Unit] = gdsSearchView.clearViewUsingTTL().map(_ => ())

  def clearDBSettingTTLRecreateView(): Future[Unit] = eventDocumentRepository
    .clearDBUsingTTL()
    .map(_ => Future.unit)

  def deleteTestEventsAndView(): Future[Unit] =
    eventDocumentRepository.deleteEvents().map(_ => gdsSearchView.deleteEvents().map(_ => ()))

  def dropViewAndRecreate(): Future[Unit] =
    val start = clock.instant()
    gdsSearchView.dropAndRecreate
    val end = clock.instant()
    logger.info(s"TestEvents gdsSearch view drop and recreate duration: ${Duration.between(start, end).toString}")
    Future.unit

  def generateTestEvents(eventCount: Int,
                         from: Instant = LocalDate.now(clock).atStartOfDay(clock.getZone).minusMonths(18).toInstant,
                         to: Instant = LocalDate.now(clock).plusDays(1).atStartOfDay(clock.getZone).minusNanos(1).toInstant,
                         randomlyGenerate: Boolean,
                         updateViewIfEnabled: Boolean = true,
                         v2EventsAvailable: Boolean = false
                        ): Future[Boolean] = {
    val start = clock.instant()
    logger.info(s"[GG-8340] TestEvents start: $start")
    insertManyTestEvents(eventCount, from, to, randomlyGenerate, updateViewIfEnabled, v2EventsAvailable)
      .map { _ =>
        val end = clock.instant
        logger.info(s"[GG-8340] TestEvents end: $end")
        logger.info(s"[GG-8340] TestEvents insert duration: ${Duration.between(start, end).toString}")
        true
      }
  }

  private def insertManyTestEvents(eventCount: Int, from: Instant, to: Instant, randomlyGenerate: Boolean, updateViewIfEnabled: Boolean, v2EventsAvailable: Boolean): Future[Unit] = {

    val eventSource: Source[Event, NotUsed] = {
      val eventsPerAccountId: Int = if randomlyGenerate then 1 else 10
      Generators.eventGeneratorStream(eventCount, eventsPerAccountId, from, to, randomlyGenerate, v2EventsAvailable)
    }

    def eventToEventDocument(event: Event): EventDocument =
      SignalProcessorEventAdapters.eventToEventDocument(event)(using clock) match {
        case Left(message) =>
          throw Exception(s"[TestEventGeneratorService] Failed to transform an Event to an EventDocument, message: $message")
        case Right(eventDocument) =>
          eventDocument
      }

    val eventDocumentInserter: Flow[EventDocument, ObjectId, NotUsed] =
      Flow[EventDocument]
        .via(StreamUtils.grouper(appConfig.testOnlyInsertGroupSize))
        .mapAsyncUnordered(appConfig.testOnlyInsertParallelism) { eventDocuments =>
          eventDocumentRepository.insertManyReturnIds(eventDocuments)
        }
        // Restrict the buffer size for the mapAsync since it deals with large groups of records
        .addAttributes(Attributes.inputBuffer(initial = 2, max = 2))
        .via(StreamUtils.ungrouper)

    val viewUpdater: Flow[ObjectId, Int, NotUsed] =
      if (appConfig.featureToggles.v1SearchUseViewEnabled && updateViewIfEnabled) {
        Flow[ObjectId]
          .via(StreamUtils.grouper(appConfig.testOnlyViewUpdateGroupSize))
          .mapAsyncUnordered(appConfig.testOnlyViewUpdateParallelism) { objectIds =>
            gdsSearchView.updateMaterializedViewForManyRecords(objectIds).map(_ => objectIds.length)
          }
      } else {
        Flow[ObjectId].map(_ => 1)
      }

    val counterSink: Sink[Int, Future[Int]] =
      Sink.fold[Int, Int](0)((total, countElement) => total + countElement)

    eventSource
      .map(eventToEventDocument)
      .via(eventDocumentInserter)
      .via(viewUpdater)
      .runWith(counterSink)
      .flatMap { count =>
        logger.info(s"[TestEventGeneratorService] Stream finished with an element count of $count")
        Future.unit
      }
  }

  private def calculatePermittedIdValues(eventCount: Int, searchResultLimit: Int): Seq[Int] = {
    val eventCountPerBatch = eventCount / searchResultLimit
    val count = if (eventCount % searchResultLimit > 0) eventCountPerBatch + 1 else eventCountPerBatch
    (1 to count).map(_ * searchResultLimit)
  }

}
