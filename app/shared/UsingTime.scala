/*
 * UsingTime.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package shared

import java.text.SimpleDateFormat
import java.util.Date

import scala.concurrent.duration.Duration
import scala.language.postfixOps

trait UsingTime extends Configured
{
  /**
   * - our formatting style
   */
  lazy val style = defaulted(config getString ("platform.compute.application.time.style"), "dd-MMM-yyyy HH:mm:ss")

  /**
   * - the pretty date formatter
   */
  lazy val formatter = new SimpleDateFormat(style)

  /**
   * - when was the actor started ?
   */
  val started = now

  def uptime = now - started

  def now = System currentTimeMillis

  def forward(lapse: Duration) = new Date(now + (lapse toMillis))

  def backward(lapse: Duration) = new Date(now - (lapse toMillis))
}
