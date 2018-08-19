/*
 * ModelParamTemplateData.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package mongo.models

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import paramscript.data.ParamScriptData

import scala.util.Try

/**
  * @author jiangliu
  *
  */
@JsonCreator
case
class ModelParamTemplateData(@JsonProperty("_id") val _id: String, val parmScript: ParamScriptData,
                             val description: ModelParamDescDataList)

@JsonCreator
case
class ModelParamDescDataList(val params: List[ModelParamDescData])

@JsonCreator
case
class ModelParamDescData(val name: String, val paramType: String,
                         val valueExpression: String)

object ModelParamTemplateDataJsonFormats {

  import play.api.libs.json._

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  implicit object ParamScriptDataFormat extends Format[ParamScriptData] {
    def writes(parmScript: ParamScriptData): JsValue = {
      Json.parse(mapper.writeValueAsString(parmScript))
    }

    def reads(json: JsValue): JsResult[ParamScriptData] = json.toString match {
      case x if x.nonEmpty => {
        val parmScript: Try[ParamScriptData] = Try(mapper.readValue[ParamScriptData](x))
        if (parmScript.isSuccess) JsSuccess(parmScript.get) else {
          JsError("Expected ParamScriptData as String")
        }
      }
      case _ => JsError("Expected ParamScriptData as String")
    }
  }

  implicit object ModelParamDescDataListFormat extends Format[ModelParamDescDataList] {
    def writes(description: ModelParamDescDataList): JsValue = {
      Json.parse(mapper.writeValueAsString(description))
    }

    def reads(json: JsValue): JsResult[ModelParamDescDataList] = json.toString match {
      case x if x.nonEmpty => {
        val description: Try[ModelParamDescDataList] = Try(mapper.readValue[ModelParamDescDataList](x))
        if (description.isSuccess) JsSuccess(description.get) else {
          JsError("Expected ModelParamDescDataList as String")
        }
      }
      case _ => JsError("Expected ModelParamDescDataList as String")
    }
  }

  implicit val modelParamTemplateDataInfoFormat: OFormat[ModelParamTemplateData] = Json.format[ModelParamTemplateData]
}
