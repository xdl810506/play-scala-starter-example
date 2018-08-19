/*
 * Decorating.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package shared

/**
  * @author jiangliu
  *
  */

import java.io.{PrintWriter, StringWriter}

import scala.language.postfixOps

trait Decorating {
  def clarify(ex: Exception): String = {
    val sw = new StringWriter
    ex.printStackTrace(new PrintWriter(sw))
    sw.toString
  }
}
