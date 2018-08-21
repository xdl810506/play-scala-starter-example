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

  def clarify(thrown: Throwable): String =
  {
    val stack = decorate(thrown) take (6) mkString (", ")

    "untrapped exception (" + (thrown getClass) + ") -> " + stack
  }

  def decorate(thrown: Throwable): Seq[String] =
  {
    val stack = thrown getStackTrace

    stack map
      {
        item =>
          (item getFileName, item getLineNumber) match
          {
            case (null, _) => "N/A"

            case (name, line) if (line >= 0) => (name + " (" + line + ")")

            case (name, _) => name
          }
      } toSeq
  }
}
