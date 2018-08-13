/*
 * BrepController.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package controllers

import akka.actor.{ActorSystem, Props}
import brephandler.brepmodel
import com.qunhe.diybe.module.math2.base.Point2d
import javax.inject._
import mongoexample.{ADDPARAMMODEL, MongoActor}
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
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
    * Creates an Action that returns a plain text message after a delay
    * of 1 second.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/message`.
    */
  def createShell = {
    Action { request => {
      val json = request.body.asJson.get
      val startPtX = (json \ "startpoint" \ "x").as[Double]
      val startPtY = (json \ "startpoint" \ "y").as[Double]
      val endPtX = (json \ "endpoint" \ "x").as[Double]
      val endPtY = (json \ "endpoint" \ "y").as[Double]
      val height = (json \ "height").as[Double]
      val (shellName, shellJson) = brepmodel.createFaceByLinearExtrusion(new Point2d(startPtX,
        startPtY),
        new Point2d(endPtX, endPtY), height)
      val outcome = Map(
        "shellName" -> shellName,
        "shellData" -> shellJson
      )
      mongoActor ! ADDPARAMMODEL(shellName, shellJson)
      Ok(JSONObject(outcome).toString())
    }
    }
  }

  private
  def getFutureMessage(delayTime: FiniteDuration): Future[String] = {
    val promise: Promise[String] = Promise[String]()
    actorSystem.scheduler.scheduleOnce(delayTime) {
      promise.success("Hi!")
    }(actorSystem.dispatcher) // run scheduled tasks using the actor system's dispatcher
    promise.future
  }

}
