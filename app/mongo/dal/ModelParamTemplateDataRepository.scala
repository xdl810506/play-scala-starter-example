/*
 * ModelParamTemplateDataRepository.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package mongo.dal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import javax.inject.{Inject, Singleton}
import mongo.data.ModelParamTemplateData
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
class ModelParamTemplateDataRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) extends Decorating {

  import mongo.data.ModelParamTemplateDataJsonFormats._

  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[ModelParamTemplateDataRepository])

  def modelParamTemplateDataInfoCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("modelParamTemplateData"))

  def getAll(limit: Int = 100): Future[Seq[ModelParamTemplateData]] =
    modelParamTemplateDataInfoCollection.flatMap(_.find(
      selector = Json.obj(/* Using Play JSON */),
      projection = Option.empty[JsObject])
      .cursor[ModelParamTemplateData](ReadPreference.primary)
      .collect[Seq](limit, Cursor.FailOnError[Seq[ModelParamTemplateData]]())
    ).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to get all model param template data from mongodb due to: " + clarify(ex))
        Seq()
    })

  def getModelParamTemplateData(id: String): Future[Option[ModelParamTemplateData]] =
    modelParamTemplateDataInfoCollection.flatMap(_.find(
      selector = BSONDocument("_id" -> id),
      projection = Option.empty[BSONDocument])
      .one[ModelParamTemplateData]).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to get model param template data from mongodb for: " + id + ", due to " + clarify(ex))
        None
    })

  def addModelParmTemplateData(modelParamTemplateDataInfo: ModelParamTemplateData): Future[WriteResult] =
    modelParamTemplateDataInfoCollection.flatMap(_.insert(modelParamTemplateDataInfo)).
      recover({
        case ex: Exception => {
          val errMessage = "Failed " +
            "to get model param template data from mongodb for: " + modelParamTemplateDataInfo._id + ", due to " + clarify(ex)
          LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", errMessage)
          LastError(true, Some(errMessage), None, None, 0, None, false, None, None, false, None, None, Seq(), None)
        }
      })

  def updateModelParamTemplateData(id: String, modelParamTemplateDataInfo: ModelParamTemplateData): Future[Option[ModelParamTemplateData]] = {
    val selector = BSONDocument("_id" -> id)
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val updateModifier = BSONDocument(
      f"$$set" -> BSONDocument(
        "parmScript" -> Json.parse(mapper.writeValueAsString(modelParamTemplateDataInfo.parmScript))),
      "description" -> Json.parse(mapper.writeValueAsString(modelParamTemplateDataInfo.description))
    )

    modelParamTemplateDataInfoCollection.flatMap(
      _.findAndUpdate(selector, updateModifier, fetchNewObject = true)
        .map(_.result[ModelParamTemplateData])
    ).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to update model param template data into mongodb for: " + id + ", due to " + clarify(ex))
        None
    })
  }

  def deleteModelParamTemplateData(id: String): Future[Option[ModelParamTemplateData]] = {
    val selector = BSONDocument("_id" -> id)
    modelParamTemplateDataInfoCollection.flatMap(_.findAndRemove(selector).map(_.result[ModelParamTemplateData])).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to delete model param template data from mongodb for: " + id + ", due to " + clarify(ex))
        None
    })
  }

}
