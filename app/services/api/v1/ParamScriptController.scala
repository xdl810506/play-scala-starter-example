/*
 * ParamScriptController.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package services.api.v1

import java.io.{PrintWriter, StringWriter}

import akka.pattern.ask
import akka.util.Timeout
import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import javax.inject._
import play.Boot
import play.api.libs.concurrent.Futures
import play.api.libs.concurrent.Futures._
import play.api.mvc._
import shared.Outcome
import subsystems.script.GET_TEMPLATE_SCRIPT

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NonFatal

/**
  * This controller creates an `Action` that demonstrates how to write
  * simple asynchronous code in a controller. It uses a timer to
  * asynchronously delay sending a response for 1 second.
  *
  * @param cc   standard controller components
  * @param exec We need an `ExecutionContext` to execute our
  *             asynchronous code.  When rendering content, you should use Play's
  *             default execution context, which is dependency injected.  If you are
  *             using blocking operations, such as database or network access, then you should
  *             use a different custom execution context that has a thread pool configured for
  *             a blocking API.
  */
@Singleton
class ParamScriptController @Inject()(cc: ControllerComponents)
                                     (implicit exec: ExecutionContext) extends AbstractController(cc) with Outcome {

  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[BrepController])
  lazy val timeoutThreshold: Long = Boot.configuration.getOptional[Long]("qunhe.geoparamengine.http" +
    ".timeout").getOrElse(30000)
  implicit val timeout: Timeout = timeoutThreshold.milliseconds

  /**
    * Creates an Action that returns a plain text message after a delay
    * of 1 second.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/message`.
    */
  def getParamTemplateScript(templateScriptId: String) = {
    Action.async { request => {
      val router = Boot.actorSystem.actorSelection("akka://application/user/daemon/router")
      val routed = (GET_TEMPLATE_SCRIPT(templateScriptId), "script")

      implicit val futures = Futures.actorSystemToFutures(Boot.actorSystem)
      (router ? routed).mapTo[Option[Map[String, String]]].withTimeout(timeoutThreshold milliseconds)
        .map(outcomeOpt => {
          outcomeOpt match {
            case Some(outcome) => {
              Ok(jsonResponse(outcome))
            }
            case _ => {
              val outcome = Map(
                "outcome" -> "no shell data found",
                "template script id" -> "",
                "shell data" -> ""
              )
              NotFound(jsonResponse(outcome))
            }
          }
        }).recover {
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
          val sw = new StringWriter
          e.printStackTrace(new PrintWriter(sw))
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", request
            .method + " " + request.uri + " " + sw.toString)
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
