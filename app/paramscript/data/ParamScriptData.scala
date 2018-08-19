/*
 * ParamScriptData.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.qunhe.diybe.module.parametric.engine.ParamScript
import com.qunhe.diybe.module.parametric.engine.nodes.{BasicFormula, BasicFunction, BasicInput}
import paramscript.{ModelParamFormula, ModelParamFunction, ModelParamInput}

import scala.collection.JavaConverters._

/**
  * @author jiangliu
  *
  */
@JsonCreator
class ParamScriptData(var formulas: List[ModelParamFormula],
                      var inputs: List[ModelParamInput],
                      var functions: List[ModelParamFunction],
                      var savedOrderedExecutionIds: List[String],
                      var savedOutputIds: Set[String]) {
  def toParamScript: ParamScript = {
    val script = new ParamScript
    script.setInputs(inputs.map((input: ModelParamInput) => input.asInstanceOf[BasicInput]).asJava)
    script
      .setFormulas(
        formulas.map((formula: ModelParamFormula) => formula.asInstanceOf[BasicFormula]).asJava)
    script.setFunctions(
      functions.map((function: ModelParamFunction) => function.asInstanceOf[BasicFunction]).asJava)
    script.setOutputs(savedOutputIds.asJava)
    script
  }

  def this() {
    this(List(), List(), List(), List(), Set())
  }
}
