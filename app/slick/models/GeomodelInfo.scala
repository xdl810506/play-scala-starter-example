/*
 * GeomodelInfo.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package slick.models

import java.sql.Timestamp

import play.api.libs.json.Json

case class GeomodelInfo(modelid: Option[Long], modeldataid: String, paramtemplateid: Long,
  modeltype: String, created: Timestamp, lastmodified: Timestamp)

object GeomodelInfo {

  //implicit val geomodelInfoFormat = Json.format[GeomodelInfo]

}


