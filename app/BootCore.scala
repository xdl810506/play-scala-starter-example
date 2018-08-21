/*
 * BootCore.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

import org.apache.logging.log4j.core.appender.routing.Routes
import org.slf4j.LoggerFactory
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.db.slick.SlickComponents
import play.api.mvc.EssentialFilter
import shared.Configured

import scala.concurrent.Future
import router.Routes

class BootCore(context: Context) extends BuiltInComponentsFromContext(context)
  with SlickComponents with Configured with play.filters.HttpFiltersComponents {

  lazy val bootLogger = LoggerFactory.getLogger("com.qunhe")

  def onStart(): Unit = {}

  def onStop(): Unit = {}

  /*val dbEncyptConfig = api.dbConfig[JdbcProfile](DbName("default"))
  val dbConfig = try {
    val decryptedConfig = dbEncyptConfig.config.
      withValue("db.user", ConfigValueFactory.fromAnyRef(dbEncyptConfig.config.getConfig("db").getString("user"))).
      withValue("db.password", ConfigValueFactory.fromAnyRef(TextDecrypter.getEncryptValue(dbEncyptConfig.config.getConfig("db").getString("password"))))
    DatabaseConfig.forConfig[JdbcProfile]("", decryptedConfig)
  } catch {
    case e: Throwable => {
      bootLogger.warn("DBConfig Encrypt error ", e)
      dbEncyptConfig
    }
  }*/
  //val dbConfig = api.dbConfig[JdbcProfile](DbName("default"))

  /*lazy val singletonScopeRepository = new ScopeRepository(dbConfig)
  lazy val singletonAlertsRepository = new AlertsRepository(dbConfig)
  lazy val singletonScriptsRepository = new ScriptsRepository(dbConfig)
  lazy val singletonLogsRepository = new LogsRepository(dbConfig)
  lazy val singletonScopejobsRepository = new ScopejobsRepository(dbConfig)
  lazy val singletonCounterRepository = new CountersRepository(dbConfig)
  lazy val singletonUsageRepository = new UsageRepository(dbConfig)
  lazy val singletonEstimatesRepository = new EstimatesRepository(dbConfig)
  lazy val singletonChannelBGDeploymentRespository = new ChannelBGDeploymentRepo(dbConfig)
  lazy val singletonBlueGreenSwitchRespository = new BlueGreenSwitchRepository(dbConfig)*/

  override def httpFilters: Seq[EssentialFilter] = {
    super.httpFilters
  }

  lazy val router = Routes

  onStart()

  applicationLifecycle.addStopHook(() => Future.successful(onStop()))

}


