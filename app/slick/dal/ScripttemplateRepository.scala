/*
 * ScripttemplateRepository.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package slick.dal

import java.sql.Timestamp

import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import shared.Decorating
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable
import slick.data.ScripttemplateInfo

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ScripttemplateRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends Decorating {
  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[ScripttemplateRepository])

  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  private class ScripttemplateTableDef(tag: Tag) extends Table[ScripttemplateInfo](tag, "scripttemplate") {
    def templateid = column[Long]("templateid", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def templatedataid = column[String]("templatedataid")

    def created = column[Timestamp]("created")

    def lastmodified = column[Timestamp]("lastmodified")

    override def * =
      (templateid.?, name, templatedataid, created, lastmodified) <> ((ScripttemplateInfo.apply _).tupled,
        ScripttemplateInfo.unapply)
  }

  private val scripttemplates = TableQuery[ScripttemplateTableDef]

  def add(scriptTemplateInfo: ScripttemplateInfo): Future[Long] = {
    val query = (scripttemplates returning scripttemplates.map(_.templateid)) += scriptTemplateInfo
    db.run(query).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to add script template info into mysql for: " + scriptTemplateInfo.templatedataid + ", due to: " + clarify(ex))
        -1
    })
  }

  def delete(templatedataid: String): Future[Int] = {
    db.run(scripttemplates.filter(_.templatedataid === templatedataid).delete).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to delete script template info from mysql for: " + templatedataid + ", due to: " + clarify(ex))
        0
    })
  }

  def get(templatedataid: String): Future[Option[ScripttemplateInfo]] = {
    db.run(scripttemplates.filter(_.templatedataid === templatedataid).result.headOption).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to get script template info from mysql for: " + templatedataid + ", due to: " + clarify(ex))
        None
    })
  }

  def listAll: Future[Seq[ScripttemplateInfo]] = {
    db.run(scripttemplates.result).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to get all script template info from mysql due to: " + clarify(ex))
        Seq()
    })
  }

  def createTable: Future[String] = {
    val schema = scripttemplates.schema
    db.run(DBIO.seq(
      MTable.getTables map (tables => {
        if (!tables.exists(_.name.name == scripttemplates.baseTableRow.tableName))
          db.run(schema.create)
      }))).map(res => "scripttemplate table successfully added").recover {
      case ex: Exception => {
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to create scripttemplate table due to: " + clarify(ex))
        ex.getCause.getMessage
      }
    }
  }
}