/*
 * ParamScriptData.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript

import com.fasterxml.jackson.annotation.JsonCreator
import com.qunhe.diybe.module.parametric.engine.ParamScript
import com.qunhe.diybe.module.parametric.engine.nodes.{BasicFormula, BasicFunction, BasicInput}

import scala.collection.JavaConverters._

/**
  * @author jiangliu
  *
  */
@JsonCreator
class ParamScriptData(var formulas: List[ParamGeoFormula],
  var inputs: List[ParamGeoInput],
  var functions: List[ParamGeoFunction],
  var savedOrderedExecutionIds: List[String],
  var savedOutputIds: Set[String]) {
  def toParamScript: ParamScript = {
    val script = new ParamScript
    script.setInputs(inputs.map((input: ParamGeoInput) => input.asInstanceOf[BasicInput]).asJava)
    script
      .setFormulas(
        formulas.map((formula: ParamGeoFormula) => formula.asInstanceOf[BasicFormula]).asJava)
    script.setFunctions(
      functions.map((function: ParamGeoFunction) => function.asInstanceOf[BasicFunction]).asJava)
    script.setOutputs(savedOutputIds.asJava)
    script
  }

  def this() {
    this(List(), List(), List(), List(), Set())
  }
}
