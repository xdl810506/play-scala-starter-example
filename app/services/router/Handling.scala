/*
 * Handling.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package services.router

import java.util.concurrent.TimeoutException

import org.apache.http.HttpStatus._
import shared.{Decorating, Supervised}

/**
  * - the idea is to catch any failure and at least pull the alarm trigger
  * - we also shoot back a 500 if we have a caller (i.e if this was triggered from a REST API)
  * - invalid input flagged by the handlers are also caught
  */
trait Handling extends Supervised with Decorating {
  implicit val routed: Option[Routed] = None

  def handle(mythen: => Unit)(implicit caller: Option[Routed]): Unit = {
    /**
      * - run the user-supplied closure
      * - any unexpected failure will be trapped and signalled back
      */
    try {
      mythen
    }
    catch {
      case e: TimeoutException => {
        val why = clarify(e)
        log.warning(self.path + " : untrapped exception -> " + why)
        caller foreach { case Routed(from, _, verified) => from ! Reply.respond(Failed(SC_BAD_GATEWAY, "Time Out"), verified)(Map("x-ads-troubleshooting" -> "Database operation time out")) }
      }
      case thrown: Throwable => {
        /**
          * - unexpected failure : trigger an alarm
          * - if we have an HTTP request, fail it on a 500 with some diagnostic
          */
        val why = clarify(thrown)

        log.error(self.path + " : untrapped exception -> " + why)

        Alert(self, why)

        caller foreach { case Routed(from, _, verified) => from ! Reply.respond(Failed(SC_INTERNAL_SERVER_ERROR, "internal failure"), verified)(Map("x-ads-troubleshooting" -> why)) }
      }
    }
  }

  def sequenced(using: String, delete: Boolean, mythen: => Unit)(implicit caller: Option[Routed]): Unit = handle(mythen)(caller)
}
