/*
 * ModelParamData.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package mongo.data

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import scala.util.Try

/**
  * @author jiangliu
  *
  */
@JsonCreator
case class ModelParamData(@JsonProperty("_id") val _id: String,
                          val paramRefData: ModelParamRefData)

@JsonCreator
case
class ModelParamRefData(val scriptTemplateId: String, val params: Map[String, String])

object ModelParamDataJsonFormats {

  import play.api.libs.json._

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  implicit object ModelParamRefDataFormat extends Format[ModelParamRefData] {
    def writes(paramRefData: ModelParamRefData): JsValue = {
      Json.parse(mapper.writeValueAsString(paramRefData))
    }

    def reads(json: JsValue): JsResult[ModelParamRefData] = json.toString match {
      case x if x.nonEmpty => {
        val paramRefData: Try[ModelParamRefData] = Try(mapper.readValue[ModelParamRefData](x))
        if (paramRefData.isSuccess) JsSuccess(paramRefData.get) else {
          JsError("Expected ModelParamRefData as String")
        }
      }
      case _ => JsError("Expected ModelParamRefData as String")
    }
  }

  implicit val modelParamDataInfoFormat: OFormat[ModelParamData] = Json.format[ModelParamData]
}
