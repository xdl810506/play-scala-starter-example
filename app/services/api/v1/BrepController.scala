/*
 * BrepController.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package services.api.v1

import java.io.{PrintWriter, StringWriter}

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.qunhe.diybe.module.math2.base.Point2d
import com.qunhe.diybe.utils.brep.topo.Shell
import com.qunhe.diybe.utils.brep.utils.BrepDataBuilder
import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import javax.inject._
import mongo.casbah.MongoActor
import mongo.dal.{ModelDataRepository, ModelParamDataRepository, ModelParamTemplateDataRepository}
import mongo.models.ModelData
import paramscript.functions.BrepFunctions
import play.Boot
import play.api.libs.concurrent.Futures
import play.api.libs.concurrent.Futures._
import play.api.libs.json.Json
import play.api.mvc._
import shared.Outcome
import slick.dal.{GeomodelRepository, ScripttemplateRepository}
import subsystems.brep.{CREATE_PARAM_MODEL, GET_PARAM_MODEL, UPDATE_PARAM_MODEL}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NonFatal

/**
  * This controller creates an `Action` that demonstrates how to write
  * simple asynchronous code in a controller. It uses a timer to
  * asynchronously delay sending a response for 1 second.
  *
  * @param cc          standard controller components
  * @param actorSystem We need the `ActorSystem`'s `Scheduler` to
  *                    run code after a delay.
  * @param exec        We need an `ExecutionContext` to execute our
  *                    asynchronous code.  When rendering content, you should use Play's
  *                    default execution context, which is dependency injected.  If you are
  *                    using blocking operations, such as database or network access, then you should
  *                    use a different custom execution context that has a thread pool configured for
  *                    a blocking API.
  */
@Singleton
class BrepController @Inject()(cc: ControllerComponents,
                               modelDataRepo: ModelDataRepository,
                               modelParamDataRepo: ModelParamDataRepository,
                               modelParamTemplateDataRepo: ModelParamTemplateDataRepository,
                               geoModelRepo: GeomodelRepository,
                               scriptTemplateRepo: ScripttemplateRepository,
                               actorSystem: ActorSystem,
                               configuration: play.api.Configuration)
                              (implicit exec: ExecutionContext) extends AbstractController(cc) with Outcome {
  lazy val mongoActor = actorSystem.actorOf(Props[MongoActor], name = "mongoActor")
  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[BrepController])
  lazy val timeoutThreshold: Long = configuration.getOptional[Long]("qunhe.geoparamengine.http" +
    ".timeout").getOrElse(30000)
  implicit val timeout: Timeout = timeoutThreshold.milliseconds

  Boot.actorSystem = actorSystem
  Boot.modelDataRepo = modelDataRepo
  Boot.modelParamDataRepo = modelParamDataRepo
  Boot.modelParamTemplateDataRepo = modelParamTemplateDataRepo
  Boot.geoModelRepo = geoModelRepo
  Boot.scriptTemplateRepo = scriptTemplateRepo
  Boot.configuration = configuration

  object ModelType extends Enumeration {
    val SHELL = "shell"
  }

  /**
    * Creates a shell from input start point, end point and extrusion height
    */
  def createShell = {
    Action.async { request => {
      // You will need a separate execution context that has been configured with enough threads to
      // deal with the expected concurrency. -- you usually get
      // that by injecting it into your controller's constructor
      // https://www.playframework.com/documentation/2.6.x/ScalaAsync
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
      modelDataRepo.addModelData(ModelData(shell.getName, shell.getName, Json.parse(modelData))).map { _ =>
        Ok(jsonResponse(outcome))
      }
    }
    }
  }

  def createParametricShell = {
    Action.async { request => {
      // You will need a separate execution context that has been configured with enough threads to
      // deal with the expected concurrency. -- you usually get
      // that by injecting it into your controller's constructor
      // https://www.playframework.com/documentation/2.6.x/ScalaAsync
      val json = request.body.asJson.get

      val router = actorSystem.actorSelection("akka://application/user/daemon/router")
      val routed = (CREATE_PARAM_MODEL(json), "brep")

      implicit val futures = Futures.actorSystemToFutures(actorSystem)
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
    Action.async { request => {
      geoModelRepo.get(shellId).flatMap(geomodelInfoOpt => {
        geomodelInfoOpt match {
          case Some(geomodelInfo) => {
            val scriptTemplateId = geomodelInfo.paramtemplatedataid
            if (scriptTemplateId.nonEmpty) {
              modelParamTemplateDataRepo.getModelParamTemplateData(scriptTemplateId).flatMap(modelParamTemplateDataOpt => {
                modelParamTemplateDataOpt match {
                  case Some(modelParamTemplateData) => {
                    val json = request.body.asJson.get
                    val router = actorSystem.actorSelection("akka://application/user/daemon/router")
                    val routed = (UPDATE_PARAM_MODEL(json, shellId, scriptTemplateId, modelParamTemplateData), "brep")

                    implicit val futures = Futures.actorSystemToFutures(actorSystem)
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
    Action.async { request => {
      val router = actorSystem.actorSelection("akka://application/user/daemon/router")
      val routed = (GET_PARAM_MODEL(shellId), "brep")

      implicit val futures = Futures.actorSystemToFutures(actorSystem)
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
