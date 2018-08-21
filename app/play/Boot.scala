/*
 * ServiceBoot.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package play

import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicReference

import akka.actor.ActorSystem
import mongo.dal.{ModelDataRepository, ModelParamDataRepository, ModelParamTemplateDataRepository}
import shared.{Loggable, UsingTime}
import slick.dal.{GeomodelRepository, ScripttemplateRepository}

/**
  * @author jiangliu
  *
  */
object Boot extends Loggable with UsingTime {
  private val systemRef = new AtomicReference[Option[ActorSystem]](None)
  private val sdf = new SimpleDateFormat("HH:mm:ss")

  var modelDataRepo: ModelDataRepository = _
  var modelParamDataRepo: ModelParamDataRepository = _
  var modelParamTemplateDataRepo: ModelParamTemplateDataRepository = _
  var geoModelRepo: GeomodelRepository = _
  var scriptTemplateRepo: ScripttemplateRepository = _

  /**
    * Gets the main actor system
    */
  def system = {
    if (systemRef.get.isEmpty) {
      systemRef.set(Some(ActorSystem("application")))
    }
    systemRef.get.get
  }

  def system_=(newValue: ActorSystem) = systemRef.set(Some(newValue))


  def hms = {
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    sdf.format(uptime)
  }

}
