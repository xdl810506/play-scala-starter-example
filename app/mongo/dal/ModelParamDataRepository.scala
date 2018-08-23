/*
 * ModelParamDataRepository.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package mongo.dal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import javax.inject.{Inject, Singleton}
import mongo.data.ModelParamData
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
class ModelParamDataRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) extends Decorating {

  import mongo.data.ModelParamDataJsonFormats._

  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[ModelParamDataRepository])

  def modelParamDataInfoCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("modelParamData"))

  def getAll(limit: Int = 100): Future[Seq[ModelParamData]] =
    modelParamDataInfoCollection.flatMap(_.find(
      selector = Json.obj(/* Using Play JSON */),
      projection = Option.empty[JsObject])
      .cursor[ModelParamData](ReadPreference.primary)
      .collect[Seq](limit, Cursor.FailOnError[Seq[ModelParamData]]())
    ).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to get all model param data from mongodb due to: " + clarify(ex))
        Seq()
    })

  def getModelParamData(id: String): Future[Option[ModelParamData]] =
    modelParamDataInfoCollection.flatMap(_.find(
      selector = BSONDocument("_id" -> id),
      projection = Option.empty[BSONDocument])
      .one[ModelParamData]).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to get model param data from mongodb for: " + id + ", due to " + clarify(ex))
        None
    })

  def addModelParmData(modelParamDataInfo: ModelParamData): Future[WriteResult] =
    modelParamDataInfoCollection.flatMap(_.insert(modelParamDataInfo)).
      recover({
        case ex: Exception => {
          val errMessage = "Failed " +
            "to get model param data from mongodb for: " + modelParamDataInfo._id + ", due to " + clarify(ex)
          LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", errMessage)
          LastError(true, Some(errMessage), None, None, 0, None, false, None, None, false, None, None, Seq(), None)
        }
      })

  def updateModelParamData(id: String, modelParamDataInfo: ModelParamData): Future[Option[ModelParamData]] = {
    val selector = BSONDocument("_id" -> id)
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val updateModifier = BSONDocument(
      f"$$set" -> BSONDocument(
        "paramRefData" -> Json.parse(mapper.writeValueAsString(modelParamDataInfo.paramRefData)))
    )

    modelParamDataInfoCollection.flatMap(
      _.findAndUpdate(selector, updateModifier, fetchNewObject = true)
        .map(_.result[ModelParamData])
    ).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to update model param data into mongodb for: " + id + ", due to " + clarify(ex))
        None
    })
  }

  def deleteModelParamData(id: String): Future[Option[ModelParamData]] = {
    val selector = BSONDocument("_id" -> id)
    modelParamDataInfoCollection.flatMap(_.findAndRemove(selector).map(_.result[ModelParamData])).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to delete model param data from mongodb for: " + id + ", due to " + clarify(ex))
        None
    })
  }

}
