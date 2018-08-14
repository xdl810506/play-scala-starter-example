/*
 * Input.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript

import com.qunhe.diybe.module.parametric.engine.nodes.BasicInput

/**
  * @author jiangliu
  *
  */
class Input(var paramName: String,
  var value: String,
  var valueType: String,
  var maxValue: String,
  var minValue: String,
  var description: String,
  var paramType: Integer,
  var restrictionType: Integer) extends BasicInput(paramName, value, valueType){
}
