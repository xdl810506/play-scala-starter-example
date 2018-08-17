/*
 * GeomodelRepository.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package slickexample.dal

import java.sql.Timestamp

import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable
import slickexample.models.{GeomodelInfo, ScopeInfo}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GeomodelRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  lazy val log = LoggerFactory.getLogger("com.qunhe")

  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  private class GeomodelTableDef(tag: Tag) extends Table[GeomodelInfo](tag, "geomodel") {
    def modelid = column[Long]("modelid", O.PrimaryKey, O.AutoInc)

    def modeldataid = column[String]("modeldataid")

    def paramtemplateid = column[Long]("paramtemplateid")

    def modeltype = column[String]("modeltype")

    def created = column[Timestamp]("created")

    def lastmodified = column[Timestamp]("lastmodified")

    override def * =
      (modelid.?, modeldataid, paramtemplateid, modeltype, created, lastmodified) <> ((GeomodelInfo.apply _).tupled,
        GeomodelInfo.unapply)
  }

  private val geomodels = TableQuery[GeomodelTableDef]

  def add(modelInfo: GeomodelInfo): Future[Boolean] = {
    db.run(geomodels += modelInfo).map(_ => true).recover({
      case ex: Exception =>
        log.warn("add model fails", ex)
        false
    })
  }

  def delete(modeldataid: String): Future[Int] = {
    db.run(geomodels.filter(_.modeldataid === modeldataid).delete)
  }

  def get(modeldataid: String): Future[Option[GeomodelInfo]] = {
    db.run(geomodels.filter(_.modeldataid === modeldataid).result.headOption).recover({
      case ex: Exception => {
        None
      }
    })
  }

  def healthCheck(): Future[Long] = {
    val reqsql = sql"SELECT modeldataid FROM geomodel limit 1".as[String]
    val auroraTick = System.currentTimeMillis()

    db.run(reqsql).map(_ => System.currentTimeMillis() - auroraTick).recover {
      case ex: Exception => {
        log.warn("healthCheck fail: " + ex.getMessage)
        -1
      }
    }
  }

  def listAll: Future[Seq[GeomodelInfo]] = {
    db.run(geomodels.result)
  }

  def createTable: Future[String] = {
    val schema = geomodels.schema
    db.run(DBIO.seq(
      MTable.getTables map (tables => {
        if (!tables.exists(_.name.name == geomodels.baseTableRow.tableName))
          db.run(schema.create)
      }))).map(res => "geomodel table successfully added").recover {
      case ex: Exception => ex.getCause.getMessage
    }
  }
}