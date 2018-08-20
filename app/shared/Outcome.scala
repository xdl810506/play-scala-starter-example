/*
 * Outcome.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package shared

import play.api.libs.json.{JsObject, JsString, Json}

/**
  * @author jiangliu
  *
  */
trait Outcome {
  def jsonResponse(messages: Map[String, String]): JsObject = {
    val fields = messages.map{ case (key, value) => (key, value:Json.JsValueWrapper) }
    Json.obj( fields.toSeq: _*)
  }
}
