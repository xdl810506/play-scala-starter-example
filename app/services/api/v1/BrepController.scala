/*
 * BrepController.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package services.api.v1

import java.io.{PrintWriter, StringWriter}

import akka.pattern.ask
import akka.util.Timeout
import com.qunhe.diybe.module.math2.base.Point2d
import com.qunhe.diybe.utils.brep.topo.Shell
import com.qunhe.diybe.utils.brep.utils.BrepDataBuilder
import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import javax.inject._
import mongo.models.ModelData
import paramscript.functions.BrepFunctions
import play.api.libs.concurrent.Futures
import play.api.libs.concurrent.Futures._
import play.api.libs.json.Json
import play.api.mvc._
import play.{Boot, Contexts}
import shared.Outcome
import subsystems.brep.{CREATE_PARAM_MODEL, GET_PARAM_MODEL, UPDATE_PARAM_MODEL}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NonFatal

/**
  * This controller creates an `Action` that demonstrates how to write
  * simple asynchronous code in a controller. It uses a timer to
  * asynchronously delay sending a response for 1 second.
  *
  * @param cc standard controller components
  */
@Singleton
class BrepController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Outcome {
  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[BrepController])
  lazy val timeoutThreshold: Long = Boot.configuration.getOptional[Long]("qunhe.geoparamengine.http" +
    ".timeout").getOrElse(30000)
  implicit val timeout: Timeout = timeoutThreshold.milliseconds
  implicit val futures = Futures.actorSystemToFutures(Boot.actorSystem)

  object ModelType extends Enumeration {
    val SHELL = "shell"
  }

  /**
    * Creates a shell from input start point, end point and extrusion height
    */
  def createShell = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Action.async { request => {
      val json = request.body.asJson.get
      val startPtX = (json \ "startpoint" \ "x").as[Double]
      val startPtY = (json \ "startpoint" \ "y").as[Double]
      val endPtX = (json \ "endpoint" \ "x").as[Double]
      val endPtY = (json \ "endpoint" \ "y").as[Double]
      val height = (json \ "height").as[Double]
      val shell: Shell = (new BrepFunctions).createFaceByLinearExtrusion(new Point2d(startPtX,
        startPtY), new Point2d(endPtX, endPtY), height)
      val outcome = Map(
        "shellName" -> shell.getName
      )
      //mongoActor ! ADD_PARAM_MODEL(shell)
      val shells: List[Shell] = List(shell)
      val modelData = BrepDataBuilder.toJson(BrepDataBuilder.buildToDatas(shells.asJava, null))
      Boot.modelDataRepo.addModelData(ModelData(shell.getName, shell.getName, Json.parse(modelData))).map { _ =>
        Ok(jsonResponse(outcome))
      }
    }
    }
  }

  def createParametricShell = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Action.async { request => {
      val json = request.body.asJson.get

      val router = Boot.actorSystem.actorSelection("akka://application/user/daemon/router")
      val routed = (CREATE_PARAM_MODEL(json), "brep")

      (router ? routed).mapTo[Map[String, String]].withTimeout(timeoutThreshold milliseconds).map {
        (outcome => Ok(jsonResponse(outcome)))
      }.recover {
        case e: scala.concurrent.TimeoutException => {
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware",
            request.method + " " + request.uri + " timeout after " + timeoutThreshold + " milliseconds")
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

  /**
    * Edit a parametric shell
    */
  def editParametricShell(shellId: String) = {
    implicit val ec = Contexts.expensiveDbLookups
    Action.async { request => {
      Boot.geoModelRepo.get(shellId).flatMap(geomodelInfoOpt => {
        geomodelInfoOpt match {
          case Some(geomodelInfo) => {
            val scriptTemplateId = geomodelInfo.paramtemplatedataid
            if (scriptTemplateId.nonEmpty) {
              Boot.modelParamTemplateDataRepo.getModelParamTemplateData(scriptTemplateId).flatMap(modelParamTemplateDataOpt => {
                modelParamTemplateDataOpt match {
                  case Some(modelParamTemplateData) => {
                    val json = request.body.asJson.get
                    val router = Boot.actorSystem.actorSelection("akka://application/user/daemon/router")
                    val routed = (UPDATE_PARAM_MODEL(json, shellId, scriptTemplateId, modelParamTemplateData), "brep")

                    (router ? routed).mapTo[Map[String, String]].withTimeout(timeoutThreshold milliseconds)
                      .map(outcome => Ok(jsonResponse(outcome)))
                      .recover {
                        case e: scala.concurrent.TimeoutException => {
                          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware",
                            request.method + " " + request.uri + " timeout after " + timeoutThreshold + " milliseconds")
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
                  case None => {
                    val outcome = Map(
                      "outcome" -> "no model param template data found"
                    )
                    scala.concurrent.Future(NotFound(jsonResponse(outcome)))
                  }
                }
              }).recover {
                case e: scala.concurrent.TimeoutException => {
                  LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware",
                    request.method + " " + request.uri + " " + "timeout after "
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
            } else {
              val outcome = Map(
                "outcome" -> "no model param template id found"
              )
              scala.concurrent.Future(NotFound(jsonResponse(outcome)))
            }
          }
          case None => {
            val outcome = Map(
              "outcome" -> "no model param data found"
            )
            scala.concurrent.Future(NotFound(jsonResponse(outcome)))
          }
        }
      }).recover {
        case e: scala.concurrent.TimeoutException => {
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", request
            .method + " " + request.uri + " " + "timeout after "
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

  /**
    * Gets a parametric shell metadata from input shell name id
    */
  def getParametricShell(shellId: String) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Action.async { request => {
      val router = Boot.actorSystem.actorSelection("akka://application/user/daemon/router")
      val routed = (GET_PARAM_MODEL(shellId), "brep")

      (router ? routed).mapTo[Option[Map[String, String]]].map(outcomeOpt => {
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
