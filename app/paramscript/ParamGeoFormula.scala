/*
 * Formula.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript

import com.fasterxml.jackson.annotation.JsonCreator
import com.qunhe.diybe.module.parametric.engine.nodes.BasicFormula

/**
  * @author jiangliu
  *
  */
@JsonCreator
class ParamGeoFormula(paramName: String,
  value: String,
  valueType: String,
  var description: String)
  extends BasicFormula(paramName, value, valueType) {

  def this(id: String,
    paramName: String,
    value: String,
    valueType: String,
    description: String) = {
    this(paramName, value, valueType, description)
    setId(id)
  }

  def this() {
    this("", "", "", "")
  }
}
