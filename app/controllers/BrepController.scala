/*
 * BrepController.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package controllers

import java.io.{PrintWriter, StringWriter}
import java.sql.Timestamp

import akka.actor.{ActorSystem, Props}
import com.qunhe.diybe.module.math2.base.Point2d
import com.qunhe.diybe.module.parametric.engine.{ParamScriptExecutor, ParamScriptResult}
import com.qunhe.diybe.utils.brep.topo.Shell
import com.qunhe.diybe.utils.brep.utils.BrepDataBuilder
import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import javax.inject._
import mongo._
import mongo.dal.{ModelDataRepository, ModelParamDataRepository, ModelParamTemplateDataRepository}
import mongo.models.ModelData
import paramscript.functions.BrepFunctions
import paramscript.helper.{ModelParamDataBuilder, ParamScriptHelper}
import play.api.libs.concurrent.Futures
import play.api.libs.concurrent.Futures._
import play.api.libs.json.Json
import play.api.mvc._
import shared.Outcome
import slick.dal.{GeomodelRepository, ScripttemplateRepository}
import slick.models.{GeomodelInfo, ScripttemplateInfo}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.parsing.json.JSONObject

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
    ".timeout").getOrElse(3000)

  object ModelType extends Enumeration{
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
      val scriptData = ParamScriptHelper.buildParamScriptDataFromJson(json)
      val executor: ParamScriptExecutor = ParamScriptHelper.paramScriptExecutor

      val resultParam: ParamScriptResult = executor.execute(scriptData.toParamScript)
      val output: String = scriptData.savedOutputIds.headOption.getOrElse("")
      val shell: Shell = resultParam.getResultMap.get(output).asInstanceOf[Shell]
      val shellName = shell.getName

      val (modelParamTemplateData, modelParamData) = ModelParamDataBuilder.
        buildModelParamAndTemplateData(shellName, scriptData)

      val shells: List[Shell] = List(shell)
      val modelData = BrepDataBuilder.toJson(BrepDataBuilder.buildToDatas(shells.asJava, null))
      val addModelDataRes = modelDataRepo.addModelData(ModelData(shellName, shellName, Json.parse(modelData)))
      val addModelParamDataRes = modelParamDataRepo.addModelParmData(modelParamData)
      val addModelParamTemplateDataRes = modelParamTemplateDataRepo.addModelParmTemplateData(modelParamTemplateData)
      val result = for {
        r1 <- addModelDataRes
        r2 <- addModelParamDataRes
        r3 <- addModelParamTemplateDataRes
      } yield (Map(
        "outcome" -> "new shell being created",
        "shell id" -> shellName
      ))

      val currentTime = new Timestamp(System.currentTimeMillis())
      scriptTemplateRepo.add(ScripttemplateInfo(None, "LinearExtrusionFace", modelParamTemplateData._id, currentTime, currentTime)).map({
        paramtemplateid => {
          geoModelRepo.add(GeomodelInfo(None, shellName, ModelType.SHELL, paramtemplateid, modelParamTemplateData._id, currentTime, currentTime))
        }
      })

      implicit val futures = Futures.actorSystemToFutures(actorSystem)
      result.withTimeout(timeoutThreshold milliseconds)
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
                    val scriptData = modelParamTemplateData.parmScript
                    val json = request.body.asJson.get
                    val userInputs = ParamScriptHelper.buildUserInputsFromJson(json)

                    val executor: ParamScriptExecutor = ParamScriptHelper.paramScriptExecutor
                    val resultParam: ParamScriptResult = executor
                      .execute(scriptData.toParamScript, userInputs.asJava)
                    val output: String = scriptData.savedOutputIds.headOption.getOrElse("")
                    val shellNew: Shell = resultParam.getResultMap.get(output).asInstanceOf[Shell]

                    val shells: List[Shell] = List(shellNew)
                    val modelData = BrepDataBuilder.toJson(BrepDataBuilder.buildToDatas(shells.asJava, null))
                    val modelDataNew = modelData.replaceAll(shellNew.getName, shellId);

                    val modelParamData = ModelParamDataBuilder.
                      buildModelParamDataWithUserInputs(shellId, scriptData,
                        userInputs, scriptTemplateId)

                    val editModelDataRes = modelDataRepo.updateModelData(shellId, ModelData(shellId, shellId, Json.parse(modelDataNew)))
                    val editModelParamDataRes = modelParamDataRepo.updateModelParamData(shellId, modelParamData)
                    val result = for {
                      r1 <- editModelDataRes
                      r2 <- editModelParamDataRes
                    } yield (Map(
                      "outcome" -> "shell being updated",
                      "shell id" -> shellId
                    ))

                    implicit val futures = Futures.actorSystemToFutures(actorSystem)
                    result.withTimeout(timeoutThreshold milliseconds)
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
    * Gets a shell from input shell name id
    */
  def getShell(shellId: String) = {
    Action.async { request => {
      implicit val futures = Futures.actorSystemToFutures(actorSystem)
      modelDataRepo.getModelData(shellId).withTimeout(timeoutThreshold milliseconds)
        .map(shelldataOpt => {
          shelldataOpt match {
            case Some(shelldata) => {
              val outcome = Map(
                "outcome" -> "shell data found",
                "shell data" -> shelldata.modelData.toString
              )
              Ok(jsonResponse(outcome))
            }
            case _ => {
              val outcome = Map(
                "outcome" -> "no shell data found"
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
