/*
 * Function.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript

import com.fasterxml.jackson.annotation.JsonCreator
import com.qunhe.diybe.module.parametric.engine.nodes.BasicFunction

import scala.collection.JavaConverters._

/**
  * @author jiangliu
  *
  */
@JsonCreator
class ParamGeoFunction(name: String,
  inputIds: Map[String, String],
  var description: String,
  functionTypeId: Int,
  versionId: Int) extends BasicFunction(name, inputIds
  .asJava) {

  def this(id: String, name: String,
    inputIds: Map[String, String],
    description: String,
    functionTypeId: Int,
    versionId: Int) = {
    this(name, inputIds, description, functionTypeId, versionId)
  }

  /**
    * NOTICE that input is name! not description
    *
    * @param name
    */
  def this(name: String) {
    this(name, Map(), "", 0, 0)
    assignUniqueId
  }

  def this() {
    this("", Map(), "", 0, 0)
    assignUniqueId
  }
}
