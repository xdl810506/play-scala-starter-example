/*
 * ServiceBoot.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

import akka.actor.{ActorRef, Props}
import org.slf4j.LoggerFactory
import play.api.ApplicationLoader.Context
import play.api.{Application, ApplicationLoader, LoggerConfigurator}
import play.{Boot, Daemon}
import shared.{Configured, Loggable}

import scala.language.postfixOps

class ServiceBoot(context: Context) extends BootCore(context) with Loggable with Configured {

  var daemon: Option[ActorRef] = None
  lazy val log = LoggerFactory.getLogger(this.getClass)

  override def onStart(): Unit = {
    super.onStart()
    Boot.system = actorSystem

    implicit val executor = scala.concurrent.ExecutionContext.Implicits.global
    daemon = Some(actorSystem.actorOf(Props(new Daemon(context.initialConfiguration)), name = "daemon"))
  }

  override def onStop(): Unit = {
    daemon.map {
      Boot.system.stop(_)
    }
    actorSystem.terminate()
    super.onStop()
  }
}

class AppLoader extends ApplicationLoader {
  override def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }
    new ServiceBoot(context).application
  }
}
