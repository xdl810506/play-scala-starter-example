/*
 * ScripttemplateInfo.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package slick.models

import java.sql.Timestamp

case
class ScripttemplateInfo(templateid: Option[Long], name: String, templatedataid: String,
  created: Timestamp, lastmodified: Timestamp)

object ScripttemplateInfo {

  //implicit val scripttemplateInfoFormat = Json.format[ScripttemplateInfo]

}



