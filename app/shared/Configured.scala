/*
 * Configured.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package shared

import java.util.UUID

import com.typesafe.config._

trait Configured
{
  lazy val config = factory

  def factory = ConfigFactory.load

  def uuid = UUID.randomUUID

  def defaulted[T](fetch: => T, default: T): T = try
  {
    fetch
  }
  catch
  {
    case _: ConfigException => default
  }

  def optional[T](fetch: => T): Option[T] = try
  {
    Some(fetch)
  }
  catch
  {
    case _: ConfigException => None
  }
}
