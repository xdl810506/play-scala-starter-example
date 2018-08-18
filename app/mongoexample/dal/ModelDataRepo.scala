/*
 * ModelDataRepo.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package mongoexample.dal

import javax.inject.{Inject, Singleton}
import mongoexample.models.ModelDataInfo
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}


/**
  * @author jiangliu
  *
  */
@Singleton
class ModelDataRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi) {

  import mongoexample.models.JsonFormats._

  def modelDataInfoCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("model"))

  def getAll(limit: Int = 100): Future[Seq[ModelDataInfo]] =
    modelDataInfoCollection.flatMap(_.find(
      selector = Json.obj(/* Using Play JSON */),
      projection = Option.empty[JsObject])
      .cursor[ModelDataInfo](ReadPreference.primary)
      .collect[Seq](limit, Cursor.FailOnError[Seq[ModelDataInfo]]())
    )

  def getModelDataInfo(id: String): Future[Option[ModelDataInfo]] =
    modelDataInfoCollection.flatMap(_.find(
      selector = BSONDocument("_id" -> id),
      projection = Option.empty[BSONDocument])
      .one[ModelDataInfo])

  def addModelDataInfo(modelDataInfo: ModelDataInfo): Future[WriteResult] =
    modelDataInfoCollection.flatMap(_.insert(modelDataInfo))

  def updateModelDataInfo(id: String, modelinfo: ModelDataInfo): Future[Option[ModelDataInfo]] = {
    val selector = BSONDocument("_id" -> id)
    val updateModifier = BSONDocument(
      f"$$set" -> BSONDocument(
        "modelId" -> modelinfo.modelId,
        "modelData" -> Json.parse(modelinfo.modelData.toString))
    )

    modelDataInfoCollection.flatMap(
      _.findAndUpdate(selector, updateModifier, fetchNewObject = true)
        .map(_.result[ModelDataInfo])
    )
  }

  def deleteModelDataInfo(id: String): Future[Option[ModelDataInfo]] = {
    val selector = BSONDocument("_id" -> id)
    modelDataInfoCollection.flatMap(_.findAndRemove(selector).map(_.result[ModelDataInfo]))
  }

}
