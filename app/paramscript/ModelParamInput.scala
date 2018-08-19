/*
 * Input.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript

import com.fasterxml.jackson.annotation.JsonCreator
import com.qunhe.diybe.module.parametric.engine.nodes.BasicInput

/**
  * @author jiangliu
  *
  */
@JsonCreator
class ModelParamInput(paramName: String,
                      value: String,
                      valueType: String,
                      var maxValue: String,
                      var minValue: String,
                      var description: String)
  extends BasicInput(paramName, value, valueType) {
  def this() {
    this("", "", "", "", "", "")
  }
}
