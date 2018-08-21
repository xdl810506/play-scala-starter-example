/*
 * Contexts.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package play

import scala.concurrent.ExecutionContext

/**
  * @author jiangliu
  *
  */
object Contexts {
  val expensiveDbLookups: ExecutionContext = Boot.actorSystem.dispatchers.lookup("akka.actor.db-lookups-dispatcher")
  val dbWriteOperations: ExecutionContext = Boot.actorSystem.dispatchers.lookup("akka.actor.db-writes-dispatcher")
  val expensiveCpuOperations: ExecutionContext = Boot.actorSystem.dispatchers.lookup("akka.actor.expensive-cpu-dispatcher")
}
