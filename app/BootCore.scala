/*
 * BootCore.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

import org.slf4j.LoggerFactory
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import shared.Configured

import scala.concurrent.Future

class BootCore(context: Context) extends BuiltInComponentsFromContext(context)
  with Configured with play.filters.HttpFiltersComponents {

  lazy val bootLogger = LoggerFactory.getLogger("com.qunhe")

  def onStart(): Unit = {}

  def onStop(): Unit = {}

  override def httpFilters: Seq[EssentialFilter] = {
    super.httpFilters
  }

  //TODO: not being able to get it working yet
  //import router.Routes
  //import com.softwaremill.macwire._
  //lazy val router = Routes
  /*override lazy val router = {
    val prefix = "/"
    wire[Routes]
  }*/
  lazy val router = Router.empty

  onStart()

  applicationLifecycle.addStopHook(() => Future.successful(onStop()))

}


