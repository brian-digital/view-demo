/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.repositories

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.{Done, NotUsed}
import org.bson.codecs.Codec
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.model.*
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.result.{DeleteResult, InsertManyResult, InsertOneResult}
import org.mongodb.scala.{Observer, WriteConcern}
import play.api.Logging
import uk.gov.hmrc.blockandsignalsgdsstore.config.EventStoreConfig
import uk.gov.hmrc.blockandsignalsgdsstore.models.*
import uk.gov.hmrc.blockandsignalsgdsstore.models.common.InitiatingEntity
import uk.gov.hmrc.blockandsignalsgdsstore.models.db.{Event, EventDocument, EventType}
import uk.gov.hmrc.blockandsignalsgdsstore.models.search.{SearchRequest, SearchResult}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.{Clock, Duration, Instant, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala

@Singleton
class EventDocumentRepository @Inject() (mongoComponent: MongoComponent, eventStoreConfig: EventStoreConfig)(implicit
  ec: ExecutionContext,
  clock: Clock
) extends PlayMongoRepository[EventDocument](
      collectionName = "events",
      mongoComponent = mongoComponent,
      domainFormat   = EventDocument.mongoFormat,
      indexes = Seq(
        IndexModel(
          ascending("event.eventId"),
          indexOptions = IndexOptions().name("eventIdIndex").unique(true)
        ),
        IndexModel(
          ascending("timeOfInterest"),
          indexOptions = IndexOptions().name("timeOfInterestIndex")
        ),
        IndexModel(
          Indexes.compoundIndex(
            Indexes.ascending("event.eventType"),
            Indexes.ascending("event.eventData.initiatingEntity")
          ),
          IndexOptions().name("eventTypeAndInitiatingEntityIndex")
        ),
        IndexModel(
          Indexes.compoundIndex(
            Indexes.ascending("event.eventType"),
            Indexes.ascending("timeOfInterest")
          ),
          IndexOptions().name("eventTypeTimeOfInterestIndex")
        ),
        IndexModel(
          Indexes.compoundIndex(
            Indexes.ascending("event.subjectId"),
            Indexes.ascending("timeOfInterest")
          ),
          IndexOptions().name("subjectIdTimeOfInterestIndex")
        ),
        IndexModel(
          Indexes.compoundIndex(
            Indexes.ascending("event.credId"),
            Indexes.ascending("timeOfInterest")
          ),
          IndexOptions().name("credIdTimeOfInterestIndex")
        ),
        IndexModel(
          Indexes.ascending("storedAt"),
          IndexOptions()
            .name("storedAtIndex")
            .expireAfter(eventStoreConfig.eventTtl.length, eventStoreConfig.eventTtl.unit)
        )
      ),
      extraCodecs = Codecs.playFormatSumCodecs(EventType.mongoFormat) :+ Codecs.playFormatCodec(SearchResult.mongoFormat)
    )
    with Logging {

  def insert(eventDocument: EventDocument): Future[Either[String, ObjectId]] = {
    logger.info(s"Inserting event - ${EventDocument.toLogString(eventDocument)}")
    collection.insertOne(eventDocument).toFuture().map { insertOneResult =>
      if insertOneResult.wasAcknowledged()
      then Right(insertOneResult.getInsertedId.asObjectId.getValue)
      else Left("Write was not acknowledged")
    }
  }

  def insertMany(eventDocuments: Seq[EventDocument]): Future[InsertManyResult] =
    collection.insertMany(eventDocuments).toFuture()

  def insertManyReturnIds(eventDocuments: Seq[EventDocument]): Future[List[ObjectId]] =
    collection
      .insertMany(eventDocuments)
      .toFuture()
      .map { insertManyResult =>
        val bsonValues = insertManyResult.getInsertedIds.values().asScala
        bsonValues.map(_.asObjectId().getValue).toList
      }

  def deleteEvents(): Future[DeleteResult] =
    collection.deleteMany(Filters.empty).toFuture()

  def clearDBUsingTTL(): Future[Done] = {
    val updates: Bson = Updates.combine(
      Updates.set("storedAt", Instant.now(clock).minus(Duration.ofNanos(eventStoreConfig.eventTtl.toNanos)))
    )
    collection
      .withWriteConcern(WriteConcern.UNACKNOWLEDGED)
      .updateMany(Filters.empty(), updates)
      .toFuture()
      .map(_ => Done)
  }

  private def buildFilter(searchRequest: SearchRequest): Bson = {

    // dateFrom: 2025-01-25
    // 2025-01-25T00:00:00Z
    val dateFromInstant = searchRequest.dateFrom
      .atStartOfDay(ZoneOffset.UTC)
      .toInstant

    // dateTo: 2025-01-25
    // 2025-01-25T23:59:59.999999999Z
    val dateToInstant = searchRequest.dateTo
      .plusDays(1)
      .atStartOfDay(ZoneOffset.UTC)
      .minusNanos(1)
      .toInstant

    val timeFilters: Seq[Bson] = Seq(
      Filters.gte("timeOfInterest", dateFromInstant),
      Filters.lte("timeOfInterest", dateToInstant)
    )

    // Always enforce AccountIntervention must be Analyst
    val accountInterventionAnalystCondition: Bson = Filters.and(
      Filters.equal("event.eventType", EventType.AccountIntervention),
      Filters.equal("event.eventData.initiatingEntity", InitiatingEntity.Analyst.toString.toLowerCase)
    )

    val eventTypeFilter: Bson = {
      searchRequest.eventType match {
        case Some(EventType.AccountIntervention) => accountInterventionAnalystCondition
        case Some(eventType)                     => Filters.equal("event.eventType", eventType)
        case None                                =>
          // For no event type filter, include all event types
          // but ensure AccountIntervention only shows Analyst-initiated
          Filters.or(
            Filters.notEqual("event.eventType", EventType.AccountIntervention),
            accountInterventionAnalystCondition
          )
      }
    }

    val mandatoryFilters: Seq[Bson] = timeFilters :+ eventTypeFilter

    val optionalFilters: Seq[Bson] = Seq(
      searchRequest.credIds.map(ids => Filters.in("event.credId", ids*)),
      searchRequest.subjectIds.map(ids => Filters.in("event.subjectId", ids*))
    ).flatten

    Filters.and((optionalFilters ++ mandatoryFilters)*)

  }

  def find(searchRequest: SearchRequest): Source[SearchResult, NotUsed] = {
    val filter: Bson = buildFilter(searchRequest)

    val searchResultsSource = collection
      .find[SearchResult](filter)
      .batchSize(eventStoreConfig.mongoSearchResultBatchSize)
      .limit(eventStoreConfig.searchResultLimit)

    val queryStart = clock.instant()
    logger.info(s"[GG-8340] EventDocumentRepository.find() start: $queryStart")
    searchResultsSource
      .subscribe(new Observer[SearchResult] {
        override def onNext(result: SearchResult): Unit = ()
        override def onError(e: Throwable): Unit = ()
        override def onComplete(): Unit = {
          val queryEnd = clock.instant()
          logger.info(s"[GG-8340] EventDocumentRepository.find() end: $queryEnd")
          logger.info(
            s"[GG-8340] EventDocumentRepository.find() duration: ${Duration.between(queryStart, queryEnd).toString}"
          )
        }
      })

    Source.fromPublisher(searchResultsSource)
  }

  def count(searchRequest: SearchRequest): Future[Long] = {
    collection.countDocuments(buildFilter(searchRequest)).toFuture()
  }

}
