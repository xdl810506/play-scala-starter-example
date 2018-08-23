/*
 * ParamScriptDataBuilder.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript.helper

import java.util.UUID

import mongo.data._
import paramscript.data._

/**
  * @author jiangliu
  *
  */
object ModelParamDataBuilder {
  def buildModelParamDataWithUserInputs(shellId: String,
                                        scriptData: ParamScriptData,
                                        userInputs: Map[String, Any],
                                        shellScriptTemplateId: String):
  ModelParamData = {
    var params: Map[String, String] = Map()
    for (inputParam <- scriptData.inputs) {
      val paramName = inputParam.getParamName
      val paramValue = userInputs.getOrElse(paramName, inputParam.getValue)
      params += (paramName -> paramValue.toString)
    }
    ModelParamData(shellId,
      ModelParamRefData(shellScriptTemplateId, params))
  }

  def buildModelParamAndTemplateData(shellId: String, scriptData: ParamScriptData):
  (ModelParamTemplateData, ModelParamData) = {
    var params: Map[String, String] = Map()
    var paramsTemplate: List[ModelParamDescData] = List()
    for (inputParam <- scriptData.inputs) {
      val paramName = inputParam.getParamName
      val paramValueType = inputParam.getValueType
      val paramValue = inputParam.getValue
      params += (paramName -> paramValue.toString)
      val scriptDescData = ModelParamDescData(paramName, paramValueType, paramValue.toString)
      paramsTemplate = scriptDescData +: paramsTemplate
    }

    val modelDataTemplateId = UUID.randomUUID.toString()
    val modelParamTemplateData: ModelParamTemplateData = ModelParamTemplateData(
      modelDataTemplateId, scriptData,
      ModelParamDescDataList(paramsTemplate))

    val modelParamData = ModelParamData(shellId, ModelParamRefData
    (modelDataTemplateId, params))

    (modelParamTemplateData, modelParamData)
  }
}
