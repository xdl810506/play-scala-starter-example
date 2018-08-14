/*
 * Point2dValuetype.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript.valuetype

import com.qunhe.diybe.module.math2.base.Point2d
import com.qunhe.diybe.module.parametric.engine.ValueType
import com.qunhe.diybe.module.parametric.engine.typeconverters.JacksonConverter

/**
  * @author jiangliu
  *
  */

class Point2dValueType(name: String)
  extends ValueType(name, classOf[Point2d], JacksonConverter.INSTANCE) {

  def this() {
    this("point2d")
  }
}
