/*
 * Dameons.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package play

import javax.inject.{Inject, Singleton}
import services.router.AppRouter
import shared.Supervised

/**
  * @author jiangliu
  *
  */
@Singleton
class Daemon @Inject()(configuration: play.api.Configuration) extends Supervised {

  //val cache = context actorOf(FromConfig.props(Props[Cache]), name = "tr-cache")

  val router = named(new AppRouter(configuration), "router")

  //val monitor = named(new Monitor(5 seconds), "monitor")

  def receive = {
    case _ =>
  }
}
