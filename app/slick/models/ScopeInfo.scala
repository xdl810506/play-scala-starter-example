/*
 * ScopeInfo.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package slick.models

import play.api.libs.json.Json

case class ScopeInfo(_id: Option[Long], scope: String, family: String)

object ScopeInfo {

  implicit val scopeInfoFormat = Json.format[ScopeInfo]

}
