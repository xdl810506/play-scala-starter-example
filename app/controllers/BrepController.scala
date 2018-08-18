/*
 * BrepController.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package controllers

import java.io.{PrintWriter, StringWriter}

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.qunhe.diybe.module.math2.base.Point2d
import com.qunhe.diybe.module.parametric.engine.{ParamScriptExecutor, ParamScriptResult}
import com.qunhe.diybe.utils.brep.topo.Shell
import com.qunhe.diybe.utils.brep.utils.BrepDataBuilder
import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import javax.inject._
import mongoexample._
import mongoexample.dal.ModelDataRepository
import mongoexample.models.ModelDataInfo
import paramscript.data.ParamScriptData
import paramscript.functions.BrepFunctions
import paramscript.helper.{ParamScriptDataBuilder, ParamScriptHelper}
import play.api.libs.concurrent.Futures
import play.api.libs.concurrent.Futures._
import play.api.libs.json.Json
import play.api.mvc._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
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
                               actorSystem: ActorSystem,
                               configuration: play.api.Configuration)
                              (implicit exec: ExecutionContext) extends AbstractController(cc) {
  lazy val mongoActor = actorSystem.actorOf(Props[MongoActor], name = "mongoActor")
  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[BrepController])
  lazy val timeoutThreshold: Long = configuration.getOptional[Long]("qunhe.geoparamengine.http" +
    ".timeout").getOrElse(3000)

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
      modelDataRepo.addModelDataInfo(ModelDataInfo(shell.getName, shell.getName, Json.parse(modelData))).map { _ =>
        Ok(JSONObject(outcome).toString())
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
      val futureOutcome = scala.concurrent.Future {
        val json = request.body.asJson.get
        val scriptData = ParamScriptDataBuilder.buildParamScriptDataFromJson(json)
        val executor: ParamScriptExecutor = ParamScriptHelper.paramScriptExecutor

        val resultParam: ParamScriptResult = executor.execute(scriptData.toParamScript)
        val output: String = scriptData.savedOutputIds.headOption.getOrElse("")
        val shell: Shell = resultParam.getResultMap.get(output).asInstanceOf[Shell]
        val outcome = Map(
          "outcome" -> "new shell being created",
          "shell id" -> shell.getName
        )

        val (scriptDataTemplate, geoScriptData) = ParamScriptDataBuilder.
          buildGeoParamScriptDataAndTemplateData(shell.getName, scriptData)
        mongoActor ! ADD_PARAM_MODEL(shell)
        mongoActor ! ADD_PARAM_TEMPLATE_SCRIPT(shell.getName, scriptDataTemplate)
        mongoActor ! ADD_PARAM_SCRIPT_DATA(shell.getName, geoScriptData)
        outcome
      }

      implicit val futures = Futures.actorSystemToFutures(actorSystem)
      futureOutcome.withTimeout(timeoutThreshold milliseconds)
        .map(outcome => Ok(JSONObject(outcome).toString()))
        .recover {
          case e: scala.concurrent.TimeoutException => {
            LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware",
              request.method + " " + request.uri + " timeout after " + timeoutThreshold + " milliseconds")
            val outcome = Map(
              "outcome" -> "request timeout"
            )
            InternalServerError(JSONObject(outcome).toString())
          }
          case NonFatal(e) => {
            val sw = new StringWriter
            e.printStackTrace(new PrintWriter(sw))
            LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", request
              .method + " " + request.uri + " " + sw.toString)
            val outcome = Map(
              "outcome" -> ("server error: " + e.getMessage)
            )
            InternalServerError(JSONObject(outcome).toString())
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
      implicit val timeout = Timeout(timeoutThreshold milliseconds)
      val futureRes: Future[Option[String]] = ask(mongoActor, GET_PARAM_SCRIPT_DATA(shellId))
        .mapTo[Option[String]]
      futureRes.flatMap(shellScriptTemplateIdOpt => {
        shellScriptTemplateIdOpt match {
          case Some(shellScriptTemplateId) if shellScriptTemplateId.nonEmpty => {
            val futureRes1: Future[Option[String]] = ask(mongoActor,
              GET_PARAM_TEMPLATE_SCRIPT(shellScriptTemplateId)).mapTo[Option[String]]
            futureRes1.map(paramScriptDataOpt => {
              paramScriptDataOpt match {
                case Some(paramScriptData) if paramScriptData.nonEmpty => {
                  val mapper = new ObjectMapper() with ScalaObjectMapper
                  mapper.registerModule(DefaultScalaModule)
                  val scriptData: ParamScriptData = mapper.readValue[ParamScriptData](
                    paramScriptData)

                  val json = request.body.asJson.get
                  val userInputs = ParamScriptDataBuilder.buildUserInputsFromJson(json)

                  val executor: ParamScriptExecutor = ParamScriptHelper.paramScriptExecutor
                  val resultParam: ParamScriptResult = executor
                    .execute(scriptData.toParamScript, userInputs.asJava)
                  val output: String = scriptData.savedOutputIds.headOption.getOrElse("")
                  val shell: Shell = resultParam.getResultMap.get(output).asInstanceOf[Shell]

                  val geoScriptData = ParamScriptDataBuilder.
                    buildGeoParamScriptDataWithUserInputs(shellId, scriptData,
                      userInputs, shellScriptTemplateId)

                  mongoActor ! EDIT_PARAM_MODEL(shellId, shell)
                  mongoActor ! EDIT_PARAM_SCRIPT_DATA(shellId, geoScriptData)

                  val outcome = Map(
                    "outcome" -> "shell being updated",
                    "shell id" -> shellId
                  )
                  Ok(JSONObject(outcome).toString())
                }
                case _ => {
                  val outcome = Map(
                    "outcome" -> "no shell template parametric script data found"
                  )
                  NotFound(JSONObject(outcome).toString())
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
                InternalServerError(JSONObject(outcome).toString())
              }
              case NonFatal(e) => {
                val sw = new StringWriter
                e.printStackTrace(new PrintWriter(sw))
                LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", request
                  .method + " " + request.uri + " " + sw.toString)
                val outcome = Map(
                  "outcome" -> ("server error: " + e.getMessage)
                )
                InternalServerError(JSONObject(outcome).toString())
              }
            }
          }
          case _ => {
            val outcome = Map(
              "outcome" -> "no shell parametric script data found"
            )
            scala.concurrent.Future(NotFound(JSONObject(outcome).toString()))
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
          InternalServerError(JSONObject(outcome).toString())
        }
        case NonFatal(e) => {
          val sw = new StringWriter
          e.printStackTrace(new PrintWriter(sw))
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", request
            .method + " " + request.uri + " " + sw.toString)
          val outcome = Map(
            "outcome" -> ("server error: " + e.getMessage)
          )
          InternalServerError(JSONObject(outcome).toString())
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
      implicit val timeout = Timeout(timeoutThreshold milliseconds)
      val futureRes: Future[Option[String]] = ask(mongoActor, GET_PARAM_MODEL(shellId))
        .mapTo[Option[String]]
      futureRes.map(shelldataOpt => {
        shelldataOpt match {
          case Some(shelldata) if shelldata.nonEmpty => {
            val outcome = Map(
              "outcome" -> "shell data found",
              "shell data" -> shelldata
            )
            Ok(JSONObject(outcome).toString())
          }
          case _ => {
            val outcome = Map(
              "outcome" -> "no shell data found"
            )
            NotFound(JSONObject(outcome).toString())
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
          InternalServerError(JSONObject(outcome).toString())
        }
        case NonFatal(e) => {
          val sw = new StringWriter
          e.printStackTrace(new PrintWriter(sw))
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", request
            .method + " " + request.uri + " " + sw.toString)
          val outcome = Map(
            "outcome" -> ("server error: " + e.getMessage)
          )
          InternalServerError(JSONObject(outcome).toString())
        }
      }
    }
    }
  }
}
