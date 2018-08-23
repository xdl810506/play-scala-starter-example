/*
 * ModelDataRepo.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package mongo.dal

import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import javax.inject.{Inject, Singleton}
import mongo.data.ModelData
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.{LastError, WriteResult}
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import shared.Decorating

import scala.concurrent.{ExecutionContext, Future}


/**
  * @author jiangliu
  *
  */
@Singleton
class ModelDataRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) extends Decorating {

  import mongo.data.ModelDataJsonFormats._

  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[ModelDataRepository])

  def modelDataInfoCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("model"))

  def getAll(limit: Int = 100): Future[Seq[ModelData]] =
    modelDataInfoCollection.flatMap(_.find(
      selector = Json.obj(/* Using Play JSON */),
      projection = Option.empty[JsObject])
      .cursor[ModelData](ReadPreference.primary)
      .collect[Seq](limit, Cursor.FailOnError[Seq[ModelData]]())
    ).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to get all model data from mongodb due to: " + clarify(ex))
        Seq()
    })

  def getModelData(id: String): Future[Option[ModelData]] =
    modelDataInfoCollection.flatMap(_.find(
      selector = BSONDocument("_id" -> id),
      projection = Option.empty[BSONDocument])
      .one[ModelData]).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to get model data from mongodb for: " + id + ", due to " + clarify(ex))
        None
    })

  def addModelData(modelDataInfo: ModelData): Future[WriteResult] =
    modelDataInfoCollection.flatMap(_.insert(modelDataInfo)).recover({
      case ex: Exception => {
        val errMessage = "Failed " +
          "to get model data from mongodb for: " + modelDataInfo._id + ", due to " + clarify(ex)
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", errMessage)
        LastError(true, Some(errMessage), None, None, 0, None, false, None, None, false, None, None, Seq(), None)
      }
    })

  def updateModelData(id: String, modelinfo: ModelData): Future[Option[ModelData]] = {
    val selector = BSONDocument("_id" -> id)
    val updateModifier = BSONDocument(
      f"$$set" -> BSONDocument(
        "modelId" -> modelinfo.modelId,
        "modelData" -> Json.parse(modelinfo.modelData.toString))
    )

    modelDataInfoCollection.flatMap(
      _.findAndUpdate(selector, updateModifier, fetchNewObject = true)
        .map(_.result[ModelData])
    ).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to update model data into mongodb for: " + id + ", due to " + clarify(ex))
        None
    })
  }

  def deleteModelData(id: String): Future[Option[ModelData]] = {
    val selector = BSONDocument("_id" -> id)
    modelDataInfoCollection.flatMap(_.findAndRemove(selector).map(_.result[ModelData])).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to delete model data from mongodb for: " + id + ", due to " + clarify(ex))
        None
    })
  }

}
