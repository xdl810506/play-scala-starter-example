/*
 * GeoParamScriptData.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript

import org.mongojack.{Id, ObjectId}

/**
  * @author jiangliu
  *
  */
class GeoParamScriptData(@Id @ObjectId var id: String, var paramScriptData: ParamScriptData) extends
  ParamScriptData(paramScriptData.formulas, paramScriptData.inputs,
    paramScriptData.functions, paramScriptData.savedOrderedExecutionIds,
    paramScriptData.savedOutputIds) {

}
