/*
 * AlertsInfo.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package slick.data

import java.sql.Timestamp

case class AlertsInfo(_id: Option[Long], from: String, logged_at: Timestamp, originator: String, reason: String)

object AlertsInfo {

}




