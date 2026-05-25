/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.repositories

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
import uk.co.tds.viewdemo.config.ComplaintsConfig
import uk.co.tds.viewdemo.models.common.InitiatingEntity
import uk.co.tds.viewdemo.models.db.ComplaintType
import uk.co.tds.viewdemo.models.search.{SearchRequest, SearchResult}
import uk.co.tds.viewdemo.models.*
import uk.co.tds.viewdemo.models.signalprocessor.v1.ComplaintRequest
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.{Clock, Duration, Instant, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala

@Singleton
class ComplaintsRepository @Inject()(mongoComponent: MongoComponent, eventStoreConfig: ComplaintsConfig)(implicit
                                                                                                         ec: ExecutionContext,
                                                                                                         clock: Clock
) extends PlayMongoRepository[ComplaintRequest](
      collectionName = "complaints",
      mongoComponent = mongoComponent,
      domainFormat   = ComplaintRequest.mongoFormat,
      indexes = Seq(
        IndexModel(
          ascending("details.subjectId"),
          indexOptions = IndexOptions().name("subjectIdIndex").unique(true)
        ),
        IndexModel(
          Indexes.compoundIndex(
            Indexes.ascending("metadata.complaintType"),
            Indexes.ascending("details.initiatingEntity")
          ),
          IndexOptions().name("complaintTypeAndInitiatingEntityIndex")
        ),
        IndexModel(
          Indexes.ascending("storedAt"),
          IndexOptions()
            .name("storedAtIndex")
            .expireAfter(eventStoreConfig.eventTtl.length, eventStoreConfig.eventTtl.unit)
        )
      ),
      extraCodecs = Codecs.playFormatSumCodecs(ComplaintType.mongoFormat) :+ Codecs.playFormatCodec(SearchResult.mongoFormat)
    )
    with Logging {

  def insert(eventDocument: ComplaintRequest): Future[Either[String, ObjectId]] = {
    collection.insertOne(eventDocument).toFuture().map { insertOneResult =>
      if insertOneResult.wasAcknowledged()
      then Right(insertOneResult.getInsertedId.asObjectId.getValue)
      else Left("Write was not acknowledged")
    }
  }

  def insertMany(complaintDocuments: Seq[ComplaintRequest]): Future[InsertManyResult] =
    collection.insertMany(complaintDocuments).toFuture()

  def insertManyReturnIds(complaintDocuments: Seq[ComplaintRequest]): Future[List[ObjectId]] =
    collection
      .insertMany(complaintDocuments)
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

}
