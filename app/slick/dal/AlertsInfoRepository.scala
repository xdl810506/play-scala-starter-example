/*
 * AlertsInfoRepository.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package slick.dal

import java.sql.Timestamp

import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import shared.Decorating
import slick.data.AlertsInfo
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class AlertsInfoRepository @Inject()(dbConfigProvider: DatabaseConfigProvider) extends Decorating {
  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[GeomodelRepository])

  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  private class AlertsInfoTableDef(tag: Tag) extends Table[AlertsInfo](tag, "alerts") {

    def _id = column[Long]("_id", O.PrimaryKey, O.AutoInc)

    def from = column[String]("from")

    def logged_at = column[Timestamp]("logged_at")

    def originator = column[String]("originator")

    def reason = column[String]("reason")

    override def * =
      (_id.?, from, logged_at, originator, reason) <> ((AlertsInfo.apply _).tupled, AlertsInfo.unapply)
  }

  private val alerts = TableQuery[AlertsInfoTableDef]

  def add(alert: AlertsInfo): Future[Boolean] = {
    db.run(alerts += alert).map(res => true).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to add alert info into mysql due to: " + clarify(ex))
        false
    })
  }

  def delete(_id: Long): Future[Int] = {
    db.run(alerts.filter(_._id === _id).delete).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to delete alert info from mysql due to: " + clarify(ex))
        0
    })
  }

  def list(since: Timestamp, sort: Int, limit: Int): Future[Seq[AlertsInfo]] = {
    db.run(alerts.filter(_.logged_at > since).sortBy(_._id.desc).take(limit).result).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to get list alerts from mysql due to: " + clarify(ex))
        Seq()
    })
  }

  def listAll: Future[Seq[AlertsInfo]] = {
    db.run(alerts.result).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to get all alerts info from mysql due to: " + clarify(ex))
        Seq()
    })
  }

  def createTable: Future[String] = {
    val schema = alerts.schema
    db.run(DBIO.seq(
      MTable.getTables map (tables => {
        if (!tables.exists(_.name.name == alerts.baseTableRow.tableName))
          db.run(schema.create)
      }))).map(res => "alerts table successfully added").recover {
      case ex: Exception => {
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to create alerts table due to: " + clarify(ex))
        ex.getCause.getMessage
      }
    }
  }
}
