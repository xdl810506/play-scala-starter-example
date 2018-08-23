/*
 * Dameons.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package play

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import mongo.dal.{ModelDataRepository, ModelParamDataRepository, ModelParamTemplateDataRepository}
import services.router.AppRouter
import shared.Supervised
import slick.dal.{AlertsInfoRepository, GeomodelRepository, ScripttemplateRepository}
import statistics.Monitor

import scala.concurrent.duration._

/**
  * @author jiangliu
  *
  */
@Singleton
class Daemon @Inject()(modelDataRepo: ModelDataRepository,
                       modelParamDataRepo: ModelParamDataRepository,
                       modelParamTemplateDataRepo: ModelParamTemplateDataRepository,
                       geoModelRepo: GeomodelRepository,
                       scriptTemplateRepo: ScripttemplateRepository,
                       alertsRepo: AlertsInfoRepository,
                       actorSystem: ActorSystem,
                       configuration: play.api.Configuration) extends Supervised {

  Boot.actorSystem = actorSystem
  Boot.modelDataRepo = modelDataRepo
  Boot.modelParamDataRepo = modelParamDataRepo
  Boot.modelParamTemplateDataRepo = modelParamTemplateDataRepo
  Boot.geoModelRepo = geoModelRepo
  Boot.scriptTemplateRepo = scriptTemplateRepo
  Boot.alertsRepo = alertsRepo
  Boot.configuration = configuration

  val router = named(new AppRouter(configuration), "router")

  val monitor = named(new Monitor(30 seconds), "monitor")

  def receive = {
    case _ =>
  }
}
