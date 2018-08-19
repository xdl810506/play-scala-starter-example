/*
 * ParamScriptHelper.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript.helper

import com.qunhe.diybe.module.parametric.engine.typeconverters.TypeConverter
import com.qunhe.diybe.module.parametric.engine.{BasicEngineModule, ParamScriptExecutor, ValueType}
import paramscript.{ModelParamFormula, ModelParamFunction, ModelParamInput}
import paramscript.data.ParamScriptData
import paramscript.functions.BrepFunctions
import paramscript.valuetype.{Point2dValueType, ShellValueType}
import play.api.libs.json.JsValue

/**
  * @author jiangliu
  *
  */
object ParamScriptHelper {
  private val module = new BasicEngineModule
  private var scriptExecutor: ParamScriptExecutor = null

  def getConverters: List[TypeConverter] = List()

  def getValueTypes: List[ValueType] = List(new Point2dValueType, new ShellValueType)

  def getFunctions: List[AnyRef] = List(new BrepFunctions)

  def enforceModule(module: BasicEngineModule): Unit = {
    getConverters.map((converter: TypeConverter) => module.addConverter(converter))
    getValueTypes.map((valueType: ValueType) => module.addValueType(valueType))
    getFunctions.map((function: Any) => module.addFunction(function))
  }

  def paramScriptExecutor = {
    if (scriptExecutor == null) {
      enforceModule(module)
      scriptExecutor = new ParamScriptExecutor(module)
    }
    scriptExecutor
  }

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

    val basicInputStartPntX = new ModelParamInput
    basicInputStartPntX.setValue(startPtX.toString)
    basicInputStartPntX.setParamName("startPtX")
    basicInputStartPntX.setValueType("double")
    basicInputStartPntX.assignUniqueId

    val basicInputStartPntY = new ModelParamInput
    basicInputStartPntY.setValue(startPtY.toString)
    basicInputStartPntY.setParamName("startPtY")
    basicInputStartPntY.setValueType("double")
    basicInputStartPntY.assignUniqueId

    val basicInputEndPntX = new ModelParamInput
    basicInputEndPntX.setValue(endPtX.toString)
    basicInputEndPntX.setParamName("endPtX")
    basicInputEndPntX.setValueType("double")
    basicInputEndPntX.assignUniqueId

    val basicInputEndPntY = new ModelParamInput
    basicInputEndPntY.setValue(endPtY.toString)
    basicInputEndPntY.setParamName("endPtY")
    basicInputEndPntY.setValueType("double")
    basicInputEndPntY.assignUniqueId

    val basicInputHeight = new ModelParamInput
    basicInputHeight.setValue(height.toString)
    basicInputHeight.setParamName("height")
    basicInputHeight.setValueType("double")
    basicInputHeight.assignUniqueId

    val inputs: List[ModelParamInput] = List(basicInputStartPntX, basicInputStartPntY,
      basicInputEndPntX, basicInputEndPntY, basicInputHeight)

    val basicFormulaStartPnt = new ModelParamFormula("startPt",
      "{\"x\":\"#startPtX\"," + "\"y\":\"#startPtY\"}", "point2d", "start point")
    basicFormulaStartPnt.assignUniqueId
    val basicFormulaEndPnt = new ModelParamFormula("endPt",
      "{\"x\":\"#endPtX\"," + "\"y\":\"#endPtY\"}", "point2d", "end point")
    basicFormulaEndPnt.assignUniqueId
    val formulas: List[ModelParamFormula] = List(basicFormulaStartPnt, basicFormulaEndPnt)

    val functionInptuts: Map[String, String] = Map("startPt" -> basicFormulaStartPnt.getId,
      "endPt" -> basicFormulaEndPnt.getId, "height" -> basicInputHeight.getId)
    val functionShell: ModelParamFunction = new ModelParamFunction(
      "BrepModeling.createFaceByLinearExtrusion", functionInptuts, "Brep function to " +
        "create shell", 0, 0)
    functionShell.assignUniqueId

    val functions: List[ModelParamFunction] = List(functionShell, functionShell)

    val output: String = functionShell.getId
    val outputs: Set[String] = Set(output)

    val scriptData = new ParamScriptData
    scriptData.formulas = formulas
    scriptData.inputs = inputs
    scriptData.functions = functions
    scriptData.savedOutputIds = outputs
    scriptData
  }
}
