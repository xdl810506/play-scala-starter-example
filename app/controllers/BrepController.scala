/*
 * BrepController.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package controllers

import java.io.{PrintWriter, StringWriter}
import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.qunhe.diybe.module.math2.base.Point2d
import com.qunhe.diybe.module.parametric.engine.{ParamScriptExecutor, ParamScriptResult}
import com.qunhe.diybe.utils.brep.topo.Shell
import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import javax.inject._
import mongoexample._
import paramscript._
import paramscript.functions.BrepFunctions
import play.api.libs.concurrent.Futures
import play.api.libs.concurrent.Futures._
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
  actorSystem: ActorSystem,
  configuration: play.api.Configuration)
  (implicit exec: ExecutionContext) extends AbstractController(cc) {
  lazy val mongoActor = actorSystem.actorOf(Props[MongoActor], name = "mongoActor")
  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[BrepController])
  lazy
  val timeoutThreshold: Long = configuration.getOptional[Long]("qunhe.geoparamengine.http" +
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
      val futureOutcome = scala.concurrent.Future {
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
        mongoActor ! ADD_PARAM_MODEL(shell)
        outcome
      }
      futureOutcome.map(outcome => Ok(JSONObject(outcome).toString()))
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
        val startPtX = (json \ "startpoint" \ "x").as[Double]
        val startPtY = (json \ "startpoint" \ "y").as[Double]
        val endPtX = (json \ "endpoint" \ "x").as[Double]
        val endPtY = (json \ "endpoint" \ "y").as[Double]
        val height = (json \ "height").as[Double]

        val basicInputStartPntX = new ParamGeoInput
        basicInputStartPntX.setValue(startPtX.toString)
        basicInputStartPntX.setParamName("startPtX")
        basicInputStartPntX.setValueType("double")
        basicInputStartPntX.assignUniqueId

        val basicInputStartPntY = new ParamGeoInput
        basicInputStartPntY.setValue(startPtY.toString)
        basicInputStartPntY.setParamName("startPtY")
        basicInputStartPntY.setValueType("double")
        basicInputStartPntY.assignUniqueId

        val basicInputEndPntX = new ParamGeoInput
        basicInputEndPntX.setValue(endPtX.toString)
        basicInputEndPntX.setParamName("endPtX")
        basicInputEndPntX.setValueType("double")
        basicInputEndPntX.assignUniqueId

        val basicInputEndPntY = new ParamGeoInput
        basicInputEndPntY.setValue(endPtY.toString)
        basicInputEndPntY.setParamName("endPtY")
        basicInputEndPntY.setValueType("double")
        basicInputEndPntY.assignUniqueId

        val basicInputHeight = new ParamGeoInput
        basicInputHeight.setValue(height.toString)
        basicInputHeight.setParamName("height")
        basicInputHeight.setValueType("double")
        basicInputHeight.assignUniqueId

        val inputs: List[ParamGeoInput] = List(basicInputStartPntX, basicInputStartPntY,
          basicInputEndPntX, basicInputEndPntY, basicInputHeight)

        val basicFormulaStartPnt = new ParamGeoFormula("startPt",
          "{\"x\":\"#startPtX\"," + "\"y\":\"#startPtY\"}", "point2d", "start point")
        basicFormulaStartPnt.assignUniqueId
        val basicFormulaEndPnt = new ParamGeoFormula("endPt",
          "{\"x\":\"#endPtX\"," + "\"y\":\"#endPtY\"}", "point2d", "end point")
        basicFormulaEndPnt.assignUniqueId
        val formulas: List[ParamGeoFormula] = List(basicFormulaStartPnt, basicFormulaEndPnt)

        val functionInptuts: Map[String, String] = Map("startPt" -> basicFormulaStartPnt.getId,
          "endPt" -> basicFormulaEndPnt.getId, "height" -> basicInputHeight.getId)
        val functionShell: ParamGeoFunction = new ParamGeoFunction(
          "BrepModeling.createFaceByLinearExtrusion", functionInptuts, "Brep function to " +
            "create shell", 0, 0)
        functionShell.assignUniqueId

        val functions: List[ParamGeoFunction] = List(functionShell, functionShell)

        val output: String = functionShell.getId
        val outputs: Set[String] = Set(output)

        val scriptData = new ParamScriptData
        scriptData.formulas = formulas
        scriptData.inputs = inputs
        scriptData.functions = functions
        scriptData.savedOutputIds = outputs

        val executor: ParamScriptExecutor = ParamScriptHelper.paramScriptExecutor

        val resultParam: ParamScriptResult = executor.execute(scriptData.toParamScript)
        val shell: Shell = resultParam.getResultMap.get(output).asInstanceOf[Shell]
        val outcome = Map(
          "outcome" -> "new shell being created",
          "shell id" -> shell.getName
        )

        val paramTemplate1 = GeoParamScriptParamDescData(basicInputStartPntX
          .getParamName, basicInputStartPntX.getValueType, basicInputStartPntX.getValue)
        val paramTemplate2 = GeoParamScriptParamDescData(basicInputStartPntY
          .getParamName, basicInputStartPntY.getValueType, basicInputStartPntY.getValue)
        val paramTemplate3 = GeoParamScriptParamDescData(basicInputEndPntX
          .getParamName, basicInputEndPntX.getValueType, basicInputEndPntX.getValue)
        val paramTemplate4 = GeoParamScriptParamDescData(basicInputEndPntY
          .getParamName, basicInputEndPntY.getValueType, basicInputEndPntY.getValue)
        val paramTemplate5 = GeoParamScriptParamDescData(basicInputHeight
          .getParamName, basicInputHeight.getValueType, basicInputHeight.getValue)
        val paramsTemplate = List(paramTemplate1, paramTemplate2, paramTemplate3, paramTemplate4,
          paramTemplate5)
        val scriptDataTemplateId = UUID.randomUUID.toString()
        val scriptDataTemplate: GeoParamScriptTemplateData = GeoParamScriptTemplateData(
          scriptDataTemplateId, scriptData,
          GeoParamScriptDescData(paramsTemplate))

        val params = Map(basicInputStartPntX.getParamName -> basicInputStartPntX.getValue,
          basicInputStartPntY.getParamName -> basicInputStartPntY.getValue,
          basicInputEndPntX.getParamName -> basicInputEndPntX.getValue,
          basicInputEndPntY.getParamName -> basicInputEndPntY.getValue,
          basicInputHeight.getParamName -> basicInputHeight.getValue)
        val geoScriptData = GeoParamScriptData(shell.getName, GeoParamScriptRefData
        (scriptDataTemplateId, params))

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
            LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", request
              .method + " " +
              request.uri +
              " timeout after " + timeoutThreshold + " milliseconds")
            InternalServerError("timeout")
          }
          case NonFatal(e)                          => {
            val sw = new StringWriter
            e.printStackTrace(new PrintWriter(sw))
            LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", request
              .method + " " + request.uri + sw.toString)
            InternalServerError(e.getMessage)
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
      val futureRes: Future[String] = ask(mongoActor, GET_PARAM_SCRIPT_DATA(shellId)).mapTo[String]
      futureRes.flatMap(shellScriptTemplateId => {
        val futureRes1: Future[String] = ask(mongoActor, GET_PARAM_TEMPLATE_SCRIPT
        (shellScriptTemplateId)).mapTo[String]
        futureRes1.map(paramScriptData => {
          val mapper = new ObjectMapper() with ScalaObjectMapper
          mapper.registerModule(DefaultScalaModule)
          val scriptData: ParamScriptData = mapper.readValue[ParamScriptData](paramScriptData)

          val json = request.body.asJson.get
          val startPtXOpt = (json \ "startpoint" \ "x").asOpt[Double]
          val startPtYOpt = (json \ "startpoint" \ "y").asOpt[Double]
          val endPtXOpt = (json \ "endpoint" \ "x").asOpt[Double]
          val endPtYOpt = (json \ "endpoint" \ "y").asOpt[Double]
          val heightOpt = (json \ "height").asOpt[Double]

          var userInputs: Map[String, Any] = Map()
          (startPtXOpt, startPtYOpt) match {
            case (Some(startPtX), Some(startPtY)) => {
              userInputs += ("startPtX" -> startPtX)
              userInputs += ("startPtY" -> startPtY)
            }
            case _                                =>
          }
          (endPtXOpt, endPtYOpt) match {
            case (Some(endPtX), Some(endPtY)) => {
              userInputs += ("endPtX" -> endPtX)
              userInputs += ("endPtY" -> endPtY)
            }
            case _                            =>
          }
          heightOpt match {
            case Some(height) => {
              userInputs += ("height" -> height)
            }
            case _            =>
          }

          val executor: ParamScriptExecutor = ParamScriptHelper.paramScriptExecutor
          val resultParam: ParamScriptResult = executor
            .execute(scriptData.toParamScript, userInputs.asJava)
          val output: String = scriptData.savedOutputIds.headOption.getOrElse("")
          val shell: Shell = resultParam.getResultMap.get(output).asInstanceOf[Shell]

          var params: Map[String, String] = Map()
          for (inputParam <- scriptData.inputs) {
            val paramName = inputParam.getParamName
            val paramValue = userInputs.getOrElse(paramName, inputParam.getValue)
            params += (paramName -> paramValue.toString)
          }
          val geoScriptData = GeoParamScriptData(shellId,
            GeoParamScriptRefData(shellScriptTemplateId, params))

          mongoActor ! EDIT_PARAM_MODEL(shellId, shell)
          mongoActor ! EDIT_PARAM_SCRIPT_DATA(shellId, geoScriptData)

          val outcome = Map(
            "outcome" -> "shell being updated",
            "shell id" -> shellId
          )
          Ok(JSONObject(outcome).toString())
        }).recover {
          case e: scala.concurrent.TimeoutException => {
            LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "", request.method + "timeout after "
              + timeoutThreshold + " milliseconds")
            InternalServerError("timeout")
          }
          case NonFatal(e)                          => {
            val sw = new StringWriter
            e.printStackTrace(new PrintWriter(sw))
            LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", request
              .method + " " + request.uri + sw.toString)
            InternalServerError(e.toString)
          }
        }
      }).recover {
        case e: scala.concurrent.TimeoutException => {
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "", request.method + "timeout after "
            + timeoutThreshold + " milliseconds")
          InternalServerError("timeout")
        }
        case NonFatal(e)                          => {
          val sw = new StringWriter
          e.printStackTrace(new PrintWriter(sw))
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", request
            .method + " " + request.uri + sw.toString)
          InternalServerError(e.getMessage)
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
      val futureRes: Future[String] = ask(mongoActor, GET_PARAM_MODEL(shellId)).mapTo[String]
      futureRes.map(shelldata => {
        val outcome = Map(
          "shell data" -> shelldata
        )
        Ok(JSONObject(outcome).toString())
      }).recover {
        case e: scala.concurrent.TimeoutException => {
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "", request.method + "timeout after "
            + timeoutThreshold + " milliseconds")
          InternalServerError("timeout")
        }
        case NonFatal(e)                          => {
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", request
            .method + " " + request.uri + e.toString)
          InternalServerError(e.getMessage)
        }
      }
    }
    }
  }
}
