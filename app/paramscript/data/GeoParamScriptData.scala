/*
 * GeoParamScriptData.scala
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
class GeoParamScriptData(@JsonProperty("_id") val id: String,
  val paramScriptRefData: GeoParamScriptRefData)

@JsonCreator
case
class GeoParamScriptRefData(val scriptTemplateId: String, val params: Map[String, String])
