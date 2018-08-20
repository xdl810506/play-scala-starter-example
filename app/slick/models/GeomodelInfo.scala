/*
 * GeomodelInfo.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package slick.models

import java.sql.Timestamp

case class GeomodelInfo(modelid: Option[Long], modeldataid: String, modeltype: String,
                        paramtemplateid: Long, paramtemplatedataid: String, created: Timestamp, lastmodified: Timestamp)

object GeomodelInfo {

  //implicit val geomodelInfoFormat = Json.format[GeomodelInfo]

}


