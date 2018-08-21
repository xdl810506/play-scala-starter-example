/*
 * AppRouter.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package services.router

import akka.actor.{Actor, ActorRef, Deploy, Props}
import akka.routing.FromConfig
import play.Boot
import shared.Supervised

import scala.collection.JavaConverters._
import scala.collection.mutable.HashMap

final case class Routed(from: ActorRef, to: String)

import javax.inject._
import play.api.Configuration

final class AppRouter @Inject()(configuration: Configuration) extends Supervised {
  /**
    * - our subsystem configuration (one block mapping to one kind of actor handling the subsystem logic)
    */
  lazy val subsystems: java.util.List[String] = try {
    configuration.underlying.getStringList("qunhe.geoparamengine.subsystems.handlers")
  } catch {
    case e: Throwable => List().asJava
  }

  /**
    * - our set of subsystem handlers
    */
  val routers = new HashMap[String, ActorRef]

  override def preStart = {
    val root = "qunhe.geoparamengine.subsystems."

    for (key <- subsystems.asScala) {
      configuration getOptional[String] (root + key + ".actor") foreach {
        path => {
          try {
            /**
              * - load the specified class
              */
            val clazz = Class.forName(path).asInstanceOf[Class[_ <: Actor]]

            val settings = config.getConfig(root + key)

            /**
              * - set it up behind a round-robin router
              */
            val pool = context.actorOf(FromConfig.props(Props(clazz).withDeploy(Deploy(config = settings))), name = key)

            routers += key -> pool

            log debug (self.path + " : added subsystem " + key + " (" + path + ")")
          }
          catch {
            case e: Throwable => {
              log warning (self.path + " : unable to setup service " + key + " (" + path + ")")
            }
          }
        }
      }
    }
  }

  def receive = {
    /**
      * - variation used to dispatch work internally (from a timer actor for instance)
      */
    case (what, toAsString: String) => {
      log debug (self.path + " : routing " + what)

      //using a forward so that the receiving actor can reply to the original sender
      routers get (toAsString) foreach {
        _ forward what
      }
    }

    case _ =>
  }
}

object AppRouter {
  lazy val router = Boot.system.actorSelection("akka://application/user/daemon/router")

  def apply(what: AnyRef, to: String): Unit = {
    val routed = (what, to)

    router ! routed
  }
}