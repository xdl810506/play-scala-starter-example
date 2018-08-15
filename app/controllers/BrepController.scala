/*
 * BrepController.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package controllers

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
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

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
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
  val LOG: QHLogger = QHLogger.getLogger(classOf[BrepController])

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
        mongoActor ! ADDPARAMMODEL(shell)
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

        val functions: List[ParamGeoFunction] = List(functionShell)

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
          "shellName" -> shell.getName
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

        mongoActor ! ADDPARAMMODEL(shell)
        mongoActor ! ADDPARAMTEMPLATESCRIPT(shell.getName, scriptDataTemplate)
        mongoActor ! ADDPARAMSCRIPTDATA(shell.getName, geoScriptData)
        outcome
      }

      implicit val futures = Futures.actorSystemToFutures(actorSystem)
      val timeoutThreshold: Long = configuration.getOptional[Long]("qunhe.geoparamengine.http.timeout").
        getOrElse(3000)
      futureOutcome.withTimeout(timeoutThreshold milliseconds)
        .map(outcome => Ok(JSONObject(outcome).toString()))
        .recover {
          case e: scala.concurrent.TimeoutException => {
            LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "", request.method + "timeout after " + timeoutThreshold + " milliseconds")
            InternalServerError("timeout")
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
      val timeoutThreshold: Long = configuration.getOptional[Long]("qunhe.geoparamengine.http.timeout").
        getOrElse(3000)
      implicit val timeout = Timeout(timeoutThreshold milliseconds)
      val futureRes: Future[String] = ask(mongoActor, GETPARAMMODEL(shellId)).mapTo[String]
      futureRes
        .map(outcome => Ok(outcome))
        .recover {
          case e: scala.concurrent.TimeoutException => {
            LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "", request.method + "timeout after " + timeoutThreshold + " milliseconds")
            InternalServerError("timeout")
          }
        }
    }
    }
  }
}
