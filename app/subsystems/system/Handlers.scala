/*
 * Handlers.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package subsystems.system

import java.util.Scanner

import play.api.libs.json._
import services.router.Handling

import scala.concurrent.Await
import scala.language.postfixOps

/**
  * - /system/v1 HTTP/REST API back-end handlers
  */
class AbstractHandlers extends Handling {

  override def factory = context.props.deploy.config

  import java.sql.Timestamp

  lazy val threshold = UsingDependencies.threshold

  lazy val debugJson: JsValue = try {
    val loader = this.getClass().getClassLoader()
    val stream = loader getResourceAsStream ("META-INF/build.json")

    try {
      val scanner = new Scanner(stream, "UTF-8").useDelimiter("\\A")
      Json parse (scanner next)
    } finally {
      stream.close()
    }
  }
  catch {
    case _: Throwable => new JsArray(Seq())
  }


  def receive = {
    case (System.DASHBOARD(window), routed@Routed(from, _, verified)) => handle({
      /**
        * - how many alerts did we get within our window
        */
      val since = backward(window)

      Boot.alertsRepository.list(new Timestamp(since.getTime()), -1, 100) map {
        alerts => {
          val count = alerts.size
          val seconds = window.toSeconds
          val rate = count * (1.0 / seconds)

          val status = rate match {
            case n if n > 1.0 => "down"
            case n if n > 0.0 => "warning"
            case _ => "up"
          }

          val alertLogs = alerts map (alert => JsString("%s @ %s : %s".format(alert.originator, alert.logged_at, alert.reason)))

          val output = Map(
            "status" -> status,
            "build info" -> debugJson,
            "alert rate" -> ("%1.0f".format(rate)),
            "alerts" -> JsArray(alertLogs.toSeq))

          from ! Reply.respond(Success(AsJson(output)), verified)
        }
      }
    })(Some(routed))

    /**
      * - Dependencies health check. (zk, mongo)
      */
    case (System.DEPENDENCIES(window), routed@Routed(from, _, verified)) => handle({

      var auroraElapsed: Long = -1


      val auroraCheck = Boot.scopeRepository.healthCheck.map(f => auroraElapsed = f)

      val healthCheck = for {
        a <- auroraCheck
      } yield {
        val output = Map(
          "aurora" -> auroraElapsed
        )

        from ! Reply.respond(Success(AsJson(output)), verified)
      }

      try {
        Await.ready(healthCheck, threshold seconds)
      } catch {
        case _: Throwable => from ! Reply.respond(Success(AsJson(Map(
          "aurora" -> auroraElapsed
        ))), verified)
      }

    })(Some(routed))
  }
}

class Handlers extends AbstractHandlers