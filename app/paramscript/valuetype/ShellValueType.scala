/*
 * ShellValueType.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript.valuetype

import com.qunhe.diybe.module.parametric.engine.ValueType
import com.qunhe.diybe.module.parametric.engine.typeconverters.JacksonConverter
import com.qunhe.diybe.utils.brep.topo.Shell

/**
  * @author jiangliu
  *
  */
class ShellValueType(name: String) extends ValueType(name, classOf[Shell], JacksonConverter.INSTANCE) {

  def this() {
    this("shell")
  }
}
