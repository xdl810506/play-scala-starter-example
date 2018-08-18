/*
 * ModelDataInfo.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package mongoexample.models

import play.api.libs.json.JsValue

/**
  * @author jiangliu
  *
  */
case class ModelDataInfo(_id: String,
                         modelId: String, modelData: JsValue)

object JsonFormats {

  import play.api.libs.json._

  /*
  import reactivemongo.play.json._
  implicit object DBObjectFormat extends Format[DBObject] {
    def writes(dbObj: DBObject): JsValue = Json.parse(dbObj.toString)
    def reads(json: JsValue): JsResult[DBObject] = json match {
      case JsString(x) => {
        val dbObj: Try[DBObject] = Try(JSON.parse(x).asInstanceOf[DBObject])
        if(dbObj.isSuccess) JsSuccess(dbObj.get) else {
          JsError("Expected DBObject as JsString")
        }
      }
      case _ => JsError("Expected DBObject as JsString")
    }
  }*/


  implicit val modelDataInfoFormat: OFormat[ModelDataInfo] = Json.format[ModelDataInfo]
}
