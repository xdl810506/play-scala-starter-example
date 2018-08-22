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
import shared.ScalaObjMapper

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

object ModelParamTemplateDataJsonFormats extends ScalaObjMapper{

  import play.api.libs.json._

  implicit object ParamScriptDataFormat extends Format[ParamScriptData] {
    def writes(parmScript: ParamScriptData): JsValue = {
      writeValueAsString(parmScript) match {
        case Some(content) => Json.parse(content)
        case _ => JsNull
      }
    }

    def reads(json: JsValue): JsResult[ParamScriptData] = json.toString match {
      case x if x.nonEmpty => {
        readValue[ParamScriptData](x) match {
          case Some(paramScriptData) => {
            val parmScript: Try[ParamScriptData] = Try(paramScriptData)
            if (parmScript.isSuccess) JsSuccess(parmScript.get) else {
              JsError("Expected ParamScriptData as String")
            }
          }
          case _ => JsError("Expected ParamScriptData as String")
        }
      }
      case _ => JsError("Expected ParamScriptData as String")
    }
  }

  implicit object ModelParamDescDataListFormat extends Format[ModelParamDescDataList] {
    def writes(description: ModelParamDescDataList): JsValue = {
      writeValueAsString(description) match {
        case Some(content) => Json.parse(content)
        case _ => JsNull
      }
    }

    def reads(json: JsValue): JsResult[ModelParamDescDataList] = json.toString match {
      case x if x.nonEmpty => {
        readValue[ModelParamDescDataList](x) match {
          case Some(modelDescDataList) => {
            val description: Try[ModelParamDescDataList] = Try(modelDescDataList)
            if (description.isSuccess) JsSuccess(description.get) else {
              JsError("Expected ModelParamDescDataList as String")
            }
          }
          case _ => JsError("Expected ModelParamDescDataList as String")
        }
      }
      case _ => JsError("Expected ModelParamDescDataList as String")
    }
  }

  implicit val modelParamTemplateDataInfoFormat: OFormat[ModelParamTemplateData] = Json.format[ModelParamTemplateData]
}
