/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.repositories

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.{Done, NotUsed}
import org.bson.codecs.Codec
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.mongodb.scala.bson.{BsonDocument, BsonTransformer}
import org.mongodb.scala.model.*
import org.mongodb.scala.model.Aggregates.{set, unset}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.result.DeleteResult
import org.mongodb.scala.{MongoException, Observer, WriteConcern}
import uk.co.tds.viewdemo.config.{AppConfig, ComplaintsConfig}
import uk.co.tds.viewdemo.models.db.ComplaintType
import uk.co.tds.viewdemo.models.search.{SearchRequest, SearchResult}
import uk.co.tds.viewdemo.services.SearchFilter
import uk.co.tds.viewdemo.models.db
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.time.{Clock, Duration, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class SearchView @Inject()(
                            mongoComponent: MongoComponent,
                            eventStoreConfig: ComplaintsConfig,
                            appConfig: AppConfig,
                            searchFilter: SearchFilter
)(implicit ec: ExecutionContext, clock: Clock)
    extends MongoMaterializedView[SearchResult](
      mongoComponent       = mongoComponent,
      viewName             = "complaintSearchView",
      sourceCollectionName = "complaints",
      domainFormat         = SearchResult.mongoFormat,
      extraCodecs          = Codecs.playFormatSumCodecs(ComplaintType.mongoFormat),
      indexes = Seq(
        IndexModel(
          ascending("storedAt"),
          IndexOptions()
            .name("storedAtIndex")
            .expireAfter(eventStoreConfig.eventTtl.length, eventStoreConfig.eventTtl.unit)
        )
      )
    ) {

  import SearchView.*

  override protected lazy val aggregationStages: Seq[Bson] = aggregatePipeline
  override protected lazy val initializeViewOnStartup: Boolean = appConfig.featureToggles.useViewEnabled

  def updateMaterializedViewForSingleRecord(objectId: ObjectId): Future[Either[String, Unit]] =
    val idFilter = Filters.equal("_id", objectId)
    updateMaterializedView(idFilter)
      .map(_ => Right(()))
      .recover { case err: MongoException =>
        val details = s"Event Mongo ID: ${objectId.toString}, Mongo error code: ${err.getCode}"
        val message = s"Failed to update search view for single record. $details"
        logger.error(message)
        Left(message)
      }

  def updateMaterializedViewForManyRecords(objectIds: List[ObjectId]): Future[Either[String, Unit]] =
    val idFilter = Filters.in("_id", objectIds*)
    updateMaterializedView(idFilter)
      .map(_ => Right(()))
      .recover { case err: MongoException =>
        val details = s"Record count: ${objectIds.length}, Mongo error code: ${err.getCode}"
        val message = s"Failed to update search view for many records. $details"
        logger.error(message)
        Left(message)
      }

  def updateMaterializedViewForAllRecords(): Future[Either[String, Unit]] =
    updateMaterializedView()
      .map(_ => Right(()))
      .recover { case err: MongoException =>
        val details = s"Mongo error code: ${err.getCode}"
        val message = s"Failed to update search view for all records. $details"
        logger.error(message)
        Left(message)
      }

  def deleteEvents(filters: Bson = Filters.empty): Future[DeleteResult] = {
    materializedView.deleteMany(filters).toFuture()
  }

  def clearViewUsingTTL(): Future[Done] = {
    val updates: Bson = Updates.combine(
      Updates.set("storedAt", Instant.now(clock).minus(Duration.ofNanos(eventStoreConfig.eventTtl.toNanos)))
    )
    materializedView
      .withWriteConcern(WriteConcern.UNACKNOWLEDGED)
      .updateMany(Filters.empty(), updates)
      .toFuture()
      .map(_ => Done)
  }

  def dropAndRecreate: Future[Try[Unit]] =
    materializedView
      .drop()
      .toFuture()
      .map(_ => initializeView)

  def find(searchRequest: SearchRequest): Source[SearchResult, NotUsed] = {
    val searchResultsSource =
      materializedView
        .find[SearchResult](searchFilter.buildFilter(searchRequest))
        .batchSize(eventStoreConfig.mongoSearchResultBatchSize)
        .limit(eventStoreConfig.searchResultLimit)

    val queryStart = clock.instant()
    logger.info(s"[VER-6642] GDSSearch view.find() start: $queryStart")

    searchResultsSource
      .subscribe(new Observer[SearchResult] {
        override def onNext(result: SearchResult): Unit = ()

        override def onError(e: Throwable): Unit = ()

        override def onComplete(): Unit = {
          val queryEnd = clock.instant()
          logger.info(s"[VER-6642] GDSSearch view.find() end: $queryEnd")
          logger.info(s"[VER-6642] GDSSearch view.find() duration: ${Duration.between(queryStart, queryEnd).toString}")
        }
      })

    Source.fromPublisher(searchResultsSource)
  }

  def findFirst(): Future[Option[SearchResult]] =
    materializedView.find[SearchResult]().limit(1).headOption()

  def count(searchRequest: SearchRequest): Future[Long] =
    materializedView.countDocuments(searchFilter.buildFilter(searchRequest)).toFuture()

}

object SearchView {

  private def reason(fields: List[String]): BsonDocument =
    fields match {
      case Nil => BsonDocument("$???")
      case last :: Nil => BsonDocument("$cond" -> BsonDocument("if" -> last, "then" -> last, "else" -> "$???"))
      case head :: tail => BsonDocument("$cond" -> BsonDocument("if" -> head, "then" -> head, "else" -> reason(tail)))
    }

  val setFields: Bson = set(
    Field("subject", "$details.subjectId"),
    Field("complaintType", "$metatData.complaintType"),
    Field("initiator", "$details.initiatingEntity"),
    Field("userId", "$details.userId"),
    Field("reason", reason(List("$details.reason", "$details.reasonAdmin", "$details.reasonUser"))),
    Field("submittedOn", "$details.eventTimestampMs")
  )

  val ignoreField: Bson = unset("metadata", "details")

  val aggregatePipeline: Seq[Bson] = Seq(setFields, ignoreField)

}
