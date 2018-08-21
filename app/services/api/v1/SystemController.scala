/*
 * SystemController.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package services.api.v1

import akka.actor.ActorSystem
import javax.inject._
import play.api.mvc._
import services.router.Routed

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

case class DASHBOARD(window: Duration)
case class DEPENDENCIES(window: Duration)

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
class SystemController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem)
                                (implicit exec: ExecutionContext) extends AbstractController(cc) {

  /**
    * Creates an Action that returns a plain text message after a delay
    * of 1 second.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/message`.
    */
  def dashboard = {
    Action.async {
      lazy val router = Boot.system.actorSelection("akka://application/user/daemons/router")
      val routed = (new DASHBOARD(1 minutes), Routed(sender, "system", verified))
      router ! routed

      getFutureMessage(1.second).map { msg => Ok(msg) }
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
