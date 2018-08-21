/*
 * Supervised.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package shared

import akka.actor.{Actor, ActorLogging, Deploy, OneForOneStrategy, Props, SupervisorStrategy}
import akka.routing.RouterConfig

import scala.language.postfixOps
import scala.concurrent.duration._

trait Supervised extends Actor with ActorLogging with Configured with UsingTime with Decorating
{
  import SupervisorStrategy.Restart

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute)
            {
              case _ => Restart
            }

  override def preStart = log.debug(self.path + " : starting")

  override def postStop = log.debug(self.path + " : stopping")

  override def preRestart(thrown: Throwable, message: Option[Any])
  {
    val cause = clarify(thrown)

    log.error(self.path + " : actor crashed, re-starting -> " + cause)

    //Alert(self, cause)
     
    context.children foreach (context.stop(_))

    postStop()
  }

  def spawn(who: => Actor) = context actorOf(Props(who).withDeploy(Deploy(config = config)))

  def named(who: => Actor, as: String) = context actorOf(Props(who).withDeploy(Deploy(config = config)), as)
}
  