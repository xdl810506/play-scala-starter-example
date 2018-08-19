/*
 * ScripttemplateRepository.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package slick.dal

import java.sql.Timestamp

import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable
import slick.models.{GeomodelInfo, ScopeInfo, ScripttemplateInfo}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ScripttemplateRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  lazy val log = LoggerFactory.getLogger("com.qunhe")

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

  def add(scriptTemplateInfo: ScripttemplateInfo): Future[Boolean] = {
    db.run(scripttemplates += scriptTemplateInfo).map(_ => true).recover({
      case ex: Exception =>
        log.warn("add script template fails", ex)
        false
    })
  }

  def delete(templatedataid: String): Future[Int] = {
    db.run(scripttemplates.filter(_.templatedataid === templatedataid).delete)
  }

  def get(templatedataid: String): Future[Option[ScripttemplateInfo]] = {
    db.run(scripttemplates.filter(_.templatedataid === templatedataid).result.headOption).recover({
      case ex: Exception => {
        None
      }
    })
  }

  def listAll: Future[Seq[ScripttemplateInfo]] = {
    db.run(scripttemplates.result)
  }

  def createTable: Future[String] = {
    val schema = scripttemplates.schema
    db.run(DBIO.seq(
      MTable.getTables map (tables => {
        if (!tables.exists(_.name.name == scripttemplates.baseTableRow.tableName))
          db.run(schema.create)
      }))).map(res => "scripttemplate table successfully added").recover {
      case ex: Exception => ex.getCause.getMessage
    }
  }
}