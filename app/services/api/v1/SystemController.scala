/*
 * SystemController.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package services.api.v1

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import javax.inject._
import play.api.libs.json.JsValue
import play.api.mvc._
import subsystems.system.DASHBOARD

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * This controller creates Actions that demonstrates system related statistics
  */
@Singleton
class SystemController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem,
                                 configuration: play.api.Configuration)
                                (implicit exec: ExecutionContext) extends AbstractController(cc) {
  lazy val timeoutThreshold: Long = configuration.getOptional[Long]("qunhe.geoparamengine.http" +
    ".timeout").getOrElse(3000)

  /**
    * Creates an Action that returns a system dashboard
    */
  def dashboard = {
    implicit val timeout: Timeout = timeoutThreshold.milliseconds
    Action.async {
      val router = actorSystem.actorSelection("akka://application/user/daemon/router")
      val routed = (DASHBOARD(1 minutes), "system")

      (router ? routed).mapTo[JsValue].map { message =>
        Ok(message)
      }
    }
  }
}
