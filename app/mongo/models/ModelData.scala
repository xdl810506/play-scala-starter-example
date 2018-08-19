/*
 * ModelData.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package mongo.models

import play.api.libs.json.JsValue

/**
  * @author jiangliu
  *
  */
case class ModelData(_id: String,
                     modelId: String, modelData: JsValue)

object ModelDataJsonFormats {

  import play.api.libs.json._

  implicit val modelDataInfoFormat: OFormat[ModelData] = Json.format[ModelData]
}
