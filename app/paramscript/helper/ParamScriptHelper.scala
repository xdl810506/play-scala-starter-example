/*
 * ParamScriptHelper.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript.helper

import com.qunhe.diybe.module.parametric.engine.typeconverters.TypeConverter
import com.qunhe.diybe.module.parametric.engine.{BasicEngineModule, ParamScriptExecutor, ValueType}
import paramscript.functions.BrepFunctions
import paramscript.valuetype.{Point2dValueType, ShellValueType}

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
}
