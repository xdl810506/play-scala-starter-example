/*
 * Loggable.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package shared

import play.api.Logger

trait Loggable {
  protected val logger = Logger(getClass)
}