/*
 * Boot.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicReference

import akka.actor.{ActorRef, ActorSystem, Props}
import org.slf4j.LoggerFactory
import play.api.ApplicationLoader.Context
import play.api.{Application, ApplicationLoader, LoggerConfigurator}
import shared.{Configured, Loggable, Supervised, UsingTime}

import scala.language.postfixOps

final class Daemons extends Supervised {

  //val cache = context actorOf(FromConfig.props(Props[Cache]), name = "tr-cache")

  //val router = named(new Router, "router")

  //val monitor = named(new Monitor(5 seconds), "monitor")

  def receive = {
    case _ =>
  }
}

/**
  * The main object representing the running application
  */
object Boot extends Loggable with UsingTime {
  private val systemRef = new AtomicReference[Option[ActorSystem]](None)
  private val sdf = new SimpleDateFormat("HH:mm:ss")
  //var scopeRepository: ScopeRepository = _

  /**
    * Gets the main actor system
    */
  def system = {
    if (systemRef.get.isEmpty) {
      systemRef.set(Some(ActorSystem("application")))
    }
    systemRef.get.get
  }

  def system_=(newValue: ActorSystem) = systemRef.set(Some(newValue))


  def hms = {
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    sdf.format(uptime)
  }

}

class ServiceBoot(context: Context) extends BootCore(context) with Loggable with Configured {

  var daemons: Option[ActorRef] = None
  lazy val log = LoggerFactory.getLogger(this.getClass)


  override def onStart(): Unit = {
    super.onStart()
    Boot.system = actorSystem
    //Boot.scopeRepository = singletonScopeRepository

    // Should create tables before start daemons actors, added for UT pass
    //
    implicit val executor = scala.concurrent.ExecutionContext.Implicits.global
    /*Boot.scopeRepository.createTable.onSuccess {
      case a => log.debug("scope table was successfully added")
     }*/

    Thread.sleep(2000L)

    daemons = Some(actorSystem.actorOf(Props(new Daemons), name = "daemons"))
  }

  override def onStop(): Unit = {
    daemons.map {
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
