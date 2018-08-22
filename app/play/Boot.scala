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
object Boot {
  var modelDataRepo: ModelDataRepository = _
  var modelParamDataRepo: ModelParamDataRepository = _
  var modelParamTemplateDataRepo: ModelParamTemplateDataRepository = _
  var geoModelRepo: GeomodelRepository = _
  var scriptTemplateRepo: ScripttemplateRepository = _
  var actorSystem: ActorSystem = _
  var configuration: play.api.Configuration = _
}
