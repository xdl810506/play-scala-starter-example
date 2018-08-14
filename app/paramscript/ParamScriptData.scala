/*
 * ParamScriptData.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript

import com.qunhe.diybe.module.parametric.engine.ParamScript
import com.qunhe.diybe.module.parametric.engine.nodes.{BasicFormula, BasicFunction, BasicInput}

import scala.collection.JavaConverters._

/**
  * @author jiangliu
  *
  */
class ParamScriptData(var formulas: List[Formula], var inputs: List[Input],
  var functions: List[Function],
  var savedOrderedExecutionIds: List[String], var savedOutputIds: Set[String]) {
  def toParamScript: ParamScript = {
    val script = new ParamScript
    script.setInputs(inputs.map((input: Input) => input.asInstanceOf[BasicInput]).asJava)
    script
      .setFormulas(formulas.map((formula: Formula) => formula.asInstanceOf[BasicFormula]).asJava)
    script.setFunctions(
      functions.map((function: Function) => function.asInstanceOf[BasicFunction]).asJava)
    script.setOutputs(savedOutputIds.asJava)
    script
  }
}
