/*
 * UsingSystemProperties.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package shared

trait UsingSystemProperties extends Configured
{
  def property(key: String, default: String): String =
  {
    System.getProperty(key) match
    {
      case null => defaulted(config.getString("platform.compute.application.properties." + key), default)

      case value => value                                           
    }
  }
}