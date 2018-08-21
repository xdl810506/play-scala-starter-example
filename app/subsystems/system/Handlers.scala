/*
 * Handlers.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package subsystems.system

import java.util.Scanner

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

  override def factory = context.props.deploy.config

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
      sender ! debugJson
    }
  }
}

class Handlers extends AbstractHandlers