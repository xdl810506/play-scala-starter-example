/*
 * Alert.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package statistics

import akka.actor.ActorRef
import play.Boot

import scala.language.postfixOps

final case class Alert(who: String, when: Long, why: String)

object Alert {
  /**
    * - our monitor
    */
  lazy val monitor = Boot.system.actorSelection("akka://application/user/daemons/monitor")

  def apply(from: ActorRef, why: String): Unit = monitor ! Alert((from path) toString, System currentTimeMillis, why)
}