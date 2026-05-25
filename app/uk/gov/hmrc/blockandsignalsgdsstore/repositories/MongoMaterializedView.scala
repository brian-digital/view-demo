/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.blockandsignalsgdsstore.repositories

import org.apache.pekko.Done
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistries
import org.bson.conversions.Bson
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Aggregates.{merge, out}
import org.mongodb.scala.model.MergeOptions.WhenMatched.KEEP_EXISTING
import org.mongodb.scala.model.{Aggregates, Filters, IndexModel, MergeOptions}
import org.mongodb.scala.{Document, MongoCollection, MongoException}
import play.api.Logging
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.logging.ObservableFutureImplicits
import uk.gov.hmrc.mongo.play.json.Codecs
import uk.gov.hmrc.mongo.{MongoComponent, MongoUtils}

import java.time.{Clock, Duration}
import java.util.concurrent.TimeoutException
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try

abstract class MongoMaterializedView[A: ClassTag](
  mongoComponent: MongoComponent,
  final val viewName: String,
  final val sourceCollectionName: String,
  final val domainFormat: Format[A],
  final val indexes: Seq[IndexModel],
  replaceIndexes: Boolean = false,
  extraCodecs: Seq[Codec[_]] = Seq.empty,
  mergeOptions: MergeOptions = MergeOptions().whenMatched(KEEP_EXISTING)
)(using ec: ExecutionContext, clock: Clock)
    extends ObservableFutureImplicits
    with Logging {

  protected lazy val aggregationStages: Seq[conversions.Bson]
  protected lazy val initializeViewOnStartup: Boolean

  protected lazy val sourceCollection: MongoCollection[Document] =
    mongoComponent.database
      .getCollection(sourceCollectionName)

  private lazy val createViewName = out(viewName)
  private lazy val updateViewName = merge(viewName, mergeOptions)

  // Helper to prevent aggregations returning ALL documents after a merge stage
  // If an aggregation uses a merge stage, it will by default return every document in the collection.
  private def safeAggregate(pipeline: Seq[Bson]): Future[Unit] =
    sourceCollection
      .aggregate(pipeline)
      .first() // Required to avoid returning ALL documents and loading them into memory
      .toFuture()
      .map(_ => ())

  lazy val materializedView: MongoCollection[A] =
    mongoComponent.database
      .getCollection[A](viewName)
      .withCodecRegistry(
        CodecRegistries.fromRegistries(
          CodecRegistries.fromCodecs(Codecs.playFormatCodec(domainFormat)),
          CodecRegistries.fromCodecs(extraCodecs*),
          DEFAULT_CODEC_REGISTRY
        )
      )

  private def setupMaterializedView: Future[Boolean] =
    for {
      collections <- mongoComponent.database.listCollectionNames().toFuture()
      exists = collections.contains(sourceCollection.namespace.getCollectionName)
      created <- if (exists) {
                   safeAggregate(aggregationStages :+ createViewName).map(_ => true)
                 } else {
                   Future.successful(false)
                 }
    } yield created

  protected def updateMaterializedView(filter: Bson = Filters.empty): Future[Either[MongoException, Unit]] = {
    val aggregateFilter = Aggregates.filter(filter)
    safeAggregate(aggregateFilter +: aggregationStages :+ updateViewName)
      .map(_ => Right(()))
      .recover { case err: MongoException =>
        logger.debug(s"Mongo view update failed with error: ${err.getMessage}")
        Left(err)
      }
  }

  private def ensureIndexes =
    MongoUtils.ensureIndexes(
      collection     = materializedView,
      indexes        = indexes,
      replaceIndexes = replaceIndexes
    )

  private def viewExists = {
    for {
      collections <- mongoComponent.database.listCollectionNames().toFuture()
    } yield collections.contains(viewName)
  }

  private def ensureView: Future[Boolean] =
    viewExists.map {
      case true  => Future.successful(true)
      case false => setupMaterializedView
    }.flatten

  protected def initialised: Future[Unit] = {
    val start = clock.instant()
    for {
      initialized <- ensureView
      _           <- ensureIndexes
    } yield logger.info(s"Collection $viewName has initialised: $initialized")
    val end = clock.instant()
    logger.info(s"Collection $viewName creation and index creation took ${Duration.between(start, end).toString}")
    Future.unit
  }

  protected def initializeView: Try[Unit] = Try(Await.result(initialised, mongoComponent.initTimeout)).recover {
    case _: TimeoutException => logger.warn(s"Index creation is taking longer than ${mongoComponent.initTimeout.toSeconds} s")
    case t: Throwable        => logger.error(s"Failed to initialise collection $viewName: ${t.getMessage}", t); throw t
  }

  if (initializeViewOnStartup) {
    logger.warn(s"initializing materialized view $viewName")
    initializeView
  }
}
