/*
 * ParamScriptDataBuilder.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript.helper

import java.util.UUID

import paramscript._
import paramscript.data._
import play.api.libs.json.JsValue

/**
  * @author qingliang
  *
  */
object ParamScriptDataBuilder {

  def buildUserInputsFromJson(json: JsValue): Map[String, Any] = {
    val startPtXOpt = (json \ "startpoint" \ "x").asOpt[Double]
    val startPtYOpt = (json \ "startpoint" \ "y").asOpt[Double]
    val endPtXOpt = (json \ "endpoint" \ "x").asOpt[Double]
    val endPtYOpt = (json \ "endpoint" \ "y").asOpt[Double]
    val heightOpt = (json \ "height").asOpt[Double]

    var userInputs: Map[String, Any] = Map()
    (startPtXOpt, startPtYOpt) match {
      case (Some(startPtX), Some(startPtY)) => {
        userInputs += ("startPtX" -> startPtX)
        userInputs += ("startPtY" -> startPtY)
      }
      case _ =>
    }
    (endPtXOpt, endPtYOpt) match {
      case (Some(endPtX), Some(endPtY)) => {
        userInputs += ("endPtX" -> endPtX)
        userInputs += ("endPtY" -> endPtY)
      }
      case _ =>
    }
    heightOpt match {
      case Some(height) => {
        userInputs += ("height" -> height)
      }
      case _ =>
    }
    userInputs
  }

  def buildParamScriptDataFromJson(json: JsValue): ParamScriptData = {
    val startPtX = (json \ "startpoint" \ "x").as[Double]
    val startPtY = (json \ "startpoint" \ "y").as[Double]
    val endPtX = (json \ "endpoint" \ "x").as[Double]
    val endPtY = (json \ "endpoint" \ "y").as[Double]
    val height = (json \ "height").as[Double]

    val basicInputStartPntX = new ParamGeoInput
    basicInputStartPntX.setValue(startPtX.toString)
    basicInputStartPntX.setParamName("startPtX")
    basicInputStartPntX.setValueType("double")
    basicInputStartPntX.assignUniqueId

    val basicInputStartPntY = new ParamGeoInput
    basicInputStartPntY.setValue(startPtY.toString)
    basicInputStartPntY.setParamName("startPtY")
    basicInputStartPntY.setValueType("double")
    basicInputStartPntY.assignUniqueId

    val basicInputEndPntX = new ParamGeoInput
    basicInputEndPntX.setValue(endPtX.toString)
    basicInputEndPntX.setParamName("endPtX")
    basicInputEndPntX.setValueType("double")
    basicInputEndPntX.assignUniqueId

    val basicInputEndPntY = new ParamGeoInput
    basicInputEndPntY.setValue(endPtY.toString)
    basicInputEndPntY.setParamName("endPtY")
    basicInputEndPntY.setValueType("double")
    basicInputEndPntY.assignUniqueId

    val basicInputHeight = new ParamGeoInput
    basicInputHeight.setValue(height.toString)
    basicInputHeight.setParamName("height")
    basicInputHeight.setValueType("double")
    basicInputHeight.assignUniqueId

    val inputs: List[ParamGeoInput] = List(basicInputStartPntX, basicInputStartPntY,
      basicInputEndPntX, basicInputEndPntY, basicInputHeight)

    val basicFormulaStartPnt = new ParamGeoFormula("startPt",
      "{\"x\":\"#startPtX\"," + "\"y\":\"#startPtY\"}", "point2d", "start point")
    basicFormulaStartPnt.assignUniqueId
    val basicFormulaEndPnt = new ParamGeoFormula("endPt",
      "{\"x\":\"#endPtX\"," + "\"y\":\"#endPtY\"}", "point2d", "end point")
    basicFormulaEndPnt.assignUniqueId
    val formulas: List[ParamGeoFormula] = List(basicFormulaStartPnt, basicFormulaEndPnt)

    val functionInptuts: Map[String, String] = Map("startPt" -> basicFormulaStartPnt.getId,
      "endPt" -> basicFormulaEndPnt.getId, "height" -> basicInputHeight.getId)
    val functionShell: ParamGeoFunction = new ParamGeoFunction(
      "BrepModeling.createFaceByLinearExtrusion", functionInptuts, "Brep function to " +
        "create shell", 0, 0)
    functionShell.assignUniqueId

    val functions: List[ParamGeoFunction] = List(functionShell, functionShell)

    val output: String = functionShell.getId
    val outputs: Set[String] = Set(output)

    val scriptData = new ParamScriptData
    scriptData.formulas = formulas
    scriptData.inputs = inputs
    scriptData.functions = functions
    scriptData.savedOutputIds = outputs
    scriptData
  }

  def buildGeoParamScriptDataWithUserInputs(shellId: String,
                                            scriptData: ParamScriptData,
                                            userInputs: Map[String, Any],
                                            shellScriptTemplateId: String):
  GeoParamScriptData = {
    var params: Map[String, String] = Map()
    for (inputParam <- scriptData.inputs) {
      val paramName = inputParam.getParamName
      val paramValue = userInputs.getOrElse(paramName, inputParam.getValue)
      params += (paramName -> paramValue.toString)
    }
    GeoParamScriptData(shellId,
      GeoParamScriptRefData(shellScriptTemplateId, params))
  }

  def buildGeoParamScriptDataAndTemplateData(shellId: String, scriptData: ParamScriptData):
  (GeoParamScriptTemplateData, GeoParamScriptData) = {
    var params: Map[String, String] = Map()
    var paramsTemplate: List[GeoParamScriptParamDescData]= List()
    for (inputParam <- scriptData.inputs) {
      val paramName = inputParam.getParamName
      val paramValueType = inputParam.getValueType
      val paramValue = inputParam.getValue
      params += (paramName -> paramValue.toString)
      val scriptDescData = GeoParamScriptParamDescData(paramName, paramValueType, paramValue.toString)
      paramsTemplate = scriptDescData +: paramsTemplate
    }

    val scriptDataTemplateId = UUID.randomUUID.toString()
    val scriptDataTemplate: GeoParamScriptTemplateData = GeoParamScriptTemplateData(
      scriptDataTemplateId, scriptData,
      GeoParamScriptDescData(paramsTemplate))

    val geoScriptData = GeoParamScriptData(shellId, GeoParamScriptRefData
    (scriptDataTemplateId, params))

    (scriptDataTemplate, geoScriptData)
  }
}
