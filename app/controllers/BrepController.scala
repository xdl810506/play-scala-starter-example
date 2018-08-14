/*
 * BrepController.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package controllers

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.qunhe.diybe.module.math2.base.Point2d
import com.qunhe.diybe.module.parametric.engine.nodes.{BasicFormula, BasicFunction, BasicInput}
import com.qunhe.diybe.module.parametric.engine.{ParamScript, ParamScriptExecutor, ParamScriptResult}
import com.qunhe.diybe.utils.brep.topo.Shell
import javax.inject._
import mongoexample.{ADDPARAMMODEL, ADDPARAMSCRIPT, GETPARAMMODEL, MongoActor}
import paramscript.ParamScriptHelper
import paramscript.functions.BrepFunctions
import play.api.mvc._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
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
class BrepController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem)
  (implicit exec: ExecutionContext) extends AbstractController(cc) {
  lazy val mongoActor = actorSystem.actorOf(Props[MongoActor], name = "mongoActor")

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

        val basicInputStartPntX = new BasicInput
        basicInputStartPntX.setValue(startPtX.toString)
        basicInputStartPntX.setParamName("startPtX")
        basicInputStartPntX.setValueType("double")
        basicInputStartPntX.assignUniqueId

        val basicInputStartPntY = new BasicInput
        basicInputStartPntY.setValue(startPtY.toString)
        basicInputStartPntY.setParamName("startPtY")
        basicInputStartPntY.setValueType("double")
        basicInputStartPntY.assignUniqueId

        val basicInputEndPntX = new BasicInput
        basicInputEndPntX.setValue(endPtX.toString)
        basicInputEndPntX.setParamName("endPtX")
        basicInputEndPntX.setValueType("double")
        basicInputEndPntX.assignUniqueId

        val basicInputEndPntY = new BasicInput
        basicInputEndPntY.setValue(endPtY.toString)
        basicInputEndPntY.setParamName("endPtY")
        basicInputEndPntY.setValueType("double")
        basicInputEndPntY.assignUniqueId

        val basicInputHeight = new BasicInput
        basicInputHeight.setValue(height.toString)
        basicInputHeight.setParamName("height")
        basicInputHeight.setValueType("double")
        basicInputHeight.assignUniqueId

        val inputs: List[BasicInput] = List(basicInputStartPntX, basicInputStartPntY,
          basicInputEndPntX, basicInputEndPntY, basicInputHeight)

        val basicFormulaStartPnt = new BasicFormula("startPt",
          "{\"x\":\"#startPtX\"," + "\"y\":\"#startPtY\"}", "point2d")
        basicFormulaStartPnt.assignUniqueId
        val basicFormulaEndPnt = new BasicFormula("endPt",
          "{\"x\":\"#endPtX\"," + "\"y\":\"#endPtY\"}", "point2d")
        basicFormulaEndPnt.assignUniqueId
        val formulas: List[BasicFormula] = List(basicFormulaStartPnt, basicFormulaEndPnt)

        val functionInptuts: Map[String, String] = Map("startPt" -> basicFormulaStartPnt.getId,
          "endPt" -> basicFormulaEndPnt.getId, "height" -> basicInputHeight.getId)
        val functionShell: BasicFunction = new BasicFunction(
          "BrepModeling.createFaceByLinearExtrusion", functionInptuts.asJava)
        functionShell.assignUniqueId

        val functions: List[BasicFunction] = List(functionShell)

        val output: String = functionShell.getId
        val outputs: Set[String] = Set(output)

        val script: ParamScript = ParamScript.builder.inputs(inputs.asJava)
          .formulas(formulas.asJava)
          .functions(functions.asJava).outputs(outputs.asJava).build
        val executor: ParamScriptExecutor = ParamScriptHelper.paramScriptExecutor

        val resultParam: ParamScriptResult = executor.execute(script)
        val shell: Shell = resultParam.getResultMap.get(output).asInstanceOf[Shell]
        val outcome = Map(
          "shellName" -> shell.getName
        )
        mongoActor ! ADDPARAMMODEL(shell)
        mongoActor ! ADDPARAMSCRIPT(shell.getName, script.toString)
        outcome
      }
      futureOutcome.map(outcome => Ok(JSONObject(outcome).toString()))
    }
    }
  }

  /**
    * Gets a shell from input shell name id
    */
  def getShell(shellId: String) = {
    Action.async { request => {
      implicit val timeout = Timeout(10 seconds)
      val futureRes: Future[String] = ask(mongoActor, GETPARAMMODEL(shellId)).mapTo[String]
      futureRes.map(outcome => Ok(outcome))
    }
    }
  }
}
