/*
 * GeoParamScriptTemplateData.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript.data

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}

/**
  * @author jiangliu
  *
  */
@JsonCreator
case
class GeoParamScriptTemplateData(@JsonProperty("_id") val id: String, val script: ParamScriptData,
  val description: GeoParamScriptDescData)

@JsonCreator
case
class GeoParamScriptDescData(val params: List[GeoParamScriptParamDescData])

@JsonCreator
case
class GeoParamScriptParamDescData(val name: String, val paramType: String,
  val valueExpression: String)

