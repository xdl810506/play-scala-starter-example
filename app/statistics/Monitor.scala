/*
 * Monitor.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package statistics

import java.sql.Timestamp

import akka.actor.Cancellable
import shared.{Supervised, UsingSystemProperties}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, _}
import scala.language.postfixOps

final class Monitor(every: Duration) extends Supervised with UsingSystemProperties {
  /**
    * - the optional node's identifier (set from the java properties, setup by Chef)
    */
  lazy val originator = property("node.id", "N/A")

  /**
    * - the alert collection in mongo
    */
  lazy val alerts = Boot.alertsRepository

  /**
    * - alert counter
    */
  var count = 0

  /**
    * - mutable scheduled cancellable (pressure estimate every x seconds)
    */
  var timer: Option[Cancellable] = None

  object Aggregate

  override def preStart = {
    log debug (self.path + " : started")

    /**
      * - setup a scheduled timer which will update mongo every x seconds
      */
    timer = Some(context.system.scheduler.scheduleOnce(1 seconds, self, Aggregate))
  }

  override def postStop = {
    log.warning(self.path + " : stopping")

    timer foreach {
      _.cancel
    }
  }

  def receive = {
    case Alert(who, tick, why) => {
      try {
        alerts.add(new Alerts(None, who, new Timestamp(tick), originator, why))
      }
      catch {
        case _: Throwable => log warning (self.path + " : unable to write alert to mongo")
      }

      count = count + 1
    }

    case Aggregate => {
      /**
        * - estimate the node's health (in our case a simple alerts per second ratio which
        * should not exceed some arbitrary threshold)
        */
      val seconds = every toSeconds

      val rate = count * (1.0 / seconds)

      val health = rate match {
        case n if n > 1.0 => 2

        case n if n > 0.0 => 1

        case _ => 0
      }

      /**
        * - grab a bunch of runtime metrics
        */
      val threads = Thread activeCount

      val runtime = Runtime getRuntime

      val total = runtime totalMemory

      val free = runtime freeMemory

      val max = runtime maxMemory

      log debug (self.path + " : " + threads + " active threads, " + total + " bytes used")

      /**
        * - reset the alert counter
        */
      count = 0
    }
  }
}
