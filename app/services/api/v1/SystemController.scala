/*
 * SystemController.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package services.api.v1

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import javax.inject._
import play.api.mvc._
import shared.{Decorating, Outcome}
import subsystems.system.DASHBOARD

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.control.NonFatal

/**
  * This controller creates Actions that demonstrates system related statistics
  */
@Singleton
class SystemController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem,
                                 configuration: play.api.Configuration)
                                (implicit exec: ExecutionContext) extends AbstractController(cc) with Outcome with Decorating {
  lazy val timeoutThreshold: Long = configuration.getOptional[Long]("qunhe.geoparamengine.http" +
    ".timeout").getOrElse(3000)
  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[SystemController])

  /**
    * Creates an Action that returns a system dashboard
    */
  def dashboard = {
    implicit val timeout: Timeout = timeoutThreshold.milliseconds
    Action.async { request => {
      val router = actorSystem.actorSelection("akka://application/user/daemon/router")
      val routed = (DASHBOARD(1 minutes), "system")

      (router ? routed).mapTo[Map[String, Any]].map { message =>
        Ok(jsonResponse(message))
      }.recover {
        case e: scala.concurrent.TimeoutException => {
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", request
            .method + " " + request.uri + " timeout after "
            + timeoutThreshold + " milliseconds")
          val outcome = Map(
            "outcome" -> "request timeout"
          )
          InternalServerError(jsonResponse(outcome))
        }
        case NonFatal(e) => {
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", request
            .method + " " + request.uri + " " + clarify(e))
          val outcome = Map(
            "outcome" -> ("server error: " + e.getMessage)
          )
          InternalServerError(jsonResponse(outcome))
        }
      }
    }
    }
  }
}
