/*
 * Router.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package services.router

import akka.actor.{Actor, ActorRef, Deploy, Props, actorRef2Scala}
import akka.routing.FromConfig
import org.apache.http.HttpStatus._
import shared.Supervised

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

final case class Routed(from: ActorRef, to: String, verified: Verified)

final class Router extends Supervised {
  /**
    * - our subsystem configuration (one block mapping to one kind of actor handling the subsystem logic)
    */
  lazy val subsystems: java.util.List[String] = defaulted(config getStringList ("platform.compute.application.subsystems"), List())

  /**
    * - our set of subsystem handlers
    */
  val routers = new HashMap[String, ActorRef]

  override def preStart = {
    val root = "platform.compute.subsystems."

    for (key <- subsystems) {
      optional(config getString (root + key + ".actor")) foreach {
        path => {
          try {
            /**
              * - the budget is used to setup a round-robing router
              */
            //                      val budget = defaulted(config getInt (root + key + ".budget"), 1)

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
            case _: Throwable => log warning (self.path + " : unable to setup service " + key + " (" + path + ")")
          }
        }
      }
    }
  }

  def receive = {
    /**
      * - requests routed from the REST/HTTP APIs carry a Tracable and are scribed
      * - the code is forgiving and will HTTP 500 if ever the requested subsystem is unknown
      */
    case (what, routed@Routed(from, to, verified@Verified(credentials, incoming))) => {
      val ms = now - incoming.tick

      log info (self.path + " : routing " + incoming.from + " > " + to + " (" + ms + " ms elapsed)")

      /**
        * - find our router
        * - fire to one of the round-robin'd actors
        */
      routers get (to) match {
        case Some(router) => router ! (what, routed)

        case None => from ! Reply.respond(Failed(SC_NOT_IMPLEMENTED, "internal failure"), verified)(Map("x-ads-troubleshooting" -> ("unknown subsystem (" + to + ")")))
      }
    }

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

object Router {
  /**
    * - our router actor
    */
  lazy val router = Boot.system.actorSelection("akka://application/user/daemons/router")

  def apply(what: AnyRef, from: ActorRef, to: String, verified: Verified): Unit = {
    val routed = (what, Routed(from, to, verified))

    router ! routed
  }

  def apply(what: AnyRef, to: String): Unit = {
    val routed = (what, to)

    router ! routed
  }
}