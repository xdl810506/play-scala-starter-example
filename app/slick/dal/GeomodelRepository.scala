/*
 * GeomodelRepository.scala
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
import slick.data.GeomodelInfo

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GeomodelRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends Decorating {
  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[GeomodelRepository])

  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  private class GeomodelTableDef(tag: Tag) extends Table[GeomodelInfo](tag, "geomodel") {
    def modelid = column[Long]("modelid", O.PrimaryKey, O.AutoInc)

    def modeldataid = column[String]("modeldataid")

    def modeltype = column[String]("modeltype")

    def paramtemplateid = column[Long]("paramtemplateid")

    def paramtemplatedataid = column[String]("paramtemplatedataid")

    def created = column[Timestamp]("created")

    def lastmodified = column[Timestamp]("lastmodified")

    override def * =
      (modelid.?, modeldataid, modeltype, paramtemplateid, paramtemplatedataid, created, lastmodified) <> ((GeomodelInfo.apply _).tupled,
        GeomodelInfo.unapply)
  }

  private val geomodels = TableQuery[GeomodelTableDef]

  def add(modelInfo: GeomodelInfo): Future[Boolean] = {
    db.run(geomodels += modelInfo).map(_ => true).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to add geometry model info into mysql for: " + modelInfo.modeldataid + ", due to: " + clarify(ex))
        false
    })
  }

  def delete(modeldataid: String): Future[Int] = {
    db.run(geomodels.filter(_.modeldataid === modeldataid).delete).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to delete geometry model info from mysql for: " + modeldataid + ", due to: " + clarify(ex))
        0
    })
  }

  def get(modeldataid: String): Future[Option[GeomodelInfo]] = {
    db.run(geomodels.filter(_.modeldataid === modeldataid).result.headOption).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to get geometry model info from mysql for: " + modeldataid + ", due to: " + clarify(ex))
        None
    })
  }

  def healthCheck(): Future[Long] = {
    val reqsql = sql"SELECT modeldataid FROM geomodel limit 1".as[String]
    val currentTick = System.currentTimeMillis()

    db.run(reqsql).map(_ => System.currentTimeMillis() - currentTick).recover {
      case ex: Exception => {
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "healthCheck failed due to: " + clarify(ex))
        -1
      }
    }
  }

  def listAll: Future[Seq[GeomodelInfo]] = {
    db.run(geomodels.result).recover({
      case ex: Exception =>
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to get all geometry model info from mysql due to: " + clarify(ex))
        Seq()
    })
  }

  def createTable: Future[String] = {
    val schema = geomodels.schema
    db.run(DBIO.seq(
      MTable.getTables map (tables => {
        if (!tables.exists(_.name.name == geomodels.baseTableRow.tableName))
          db.run(schema.create)
      }))).map(res => "geomodel table successfully added").recover {
      case ex: Exception => {
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
          "to create geomodel table due to: " + clarify(ex))
        ex.getCause.getMessage
      }
    }
  }
}