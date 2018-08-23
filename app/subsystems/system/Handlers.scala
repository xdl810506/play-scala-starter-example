/*
 * Handlers.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package subsystems.system

import akka.pattern.pipe
import java.sql.Timestamp
import java.util.Scanner

import play.{Boot, Contexts}
import play.api.libs.json._
import shared.Supervised

import scala.concurrent.duration.Duration
import scala.language.postfixOps

case class DASHBOARD(window: Duration)

case class DEPENDENCIES(window: Duration)

/**
  * - /system/v1 HTTP/REST System API back-end handlers
  */
class AbstractHandlers extends Supervised {

  implicit val ec = Contexts.expensiveDbLookups

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
    case _: Throwable => new JsString("")
  }


  override
  def receive = {
    case (DASHBOARD(window)) => {
      /**
        * - how many alerts did we get within our window
        */
      val since = backward(window)

      val result = Boot.alertsRepo.list(new Timestamp(since.getTime()), -1, 100) map (
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

          Map(
            "status" -> status,
            "build info" -> debugJson,
            "alert rate" -> ("%1.0f".format(rate)),
            "alerts" -> JsArray(alertLogs))
        })

      result.pipeTo(sender)
    }
  }
}

class Handlers extends AbstractHandlers