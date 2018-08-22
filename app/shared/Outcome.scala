/*
 * Outcome.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package shared

import play.api.libs.json.{JsObject, JsValue, Json}

/**
  * @author jiangliu
  *
  */
trait Outcome {
  def jsonResponse(messages: Map[String, Any]): JsObject = {
    val fields = messages.map {
      case (key, value: String) => (key, value: Json.JsValueWrapper)
      case (key, value: JsValue) => (key, value: Json.JsValueWrapper)
      case (key, value) => (key, value.toString: Json.JsValueWrapper)
    }
    Json.obj(fields.toSeq: _*)
  }
}
