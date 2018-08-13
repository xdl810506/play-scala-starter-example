/*
 * ScopeRepository.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package slickexample.dal

import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable
import slickexample.models.ScopeInfo

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ScopeRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  lazy val log = LoggerFactory.getLogger("com.qunhe")

  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  private class ScopeTableDef(tag: Tag) extends Table[ScopeInfo](tag, "scope") {
    def _id = column[Long]("_id", O.PrimaryKey, O.AutoInc)

    def scope = column[String]("scope")

    def family = column[String]("family")

    override def * =
      (_id.?, scope, family) <> ((ScopeInfo.apply _).tupled, ScopeInfo.unapply)
  }

  private val scopeInfos = TableQuery[ScopeTableDef]

  def add(scopeInfo: ScopeInfo): Future[String] = {
    db.run(scopeInfos += scopeInfo).map(res => "scope info successfully added").recover {
      case ex: Exception => {
        log.warn("db write error." + ex.getMessage)
        throw ex
        ex.getMessage
      }
    }
  }

  def delete(scope: String): Future[Int] = {
    db.run(scopeInfos.filter(_.scope === scope).delete)
  }

  def get(scope: String): Future[Option[ScopeInfo]] = {
    db.run(scopeInfos.filter(_.scope === scope).result.headOption).recover({
      case ex: Exception => {
        None
      }
    })
  }

  def healthCheck(): Future[Long] = {
    val reqsql = sql"SELECT scope FROM scope limit 1".as[String]
    val auroraTick = System.currentTimeMillis()

    db.run(reqsql).map(_ => System.currentTimeMillis() - auroraTick).recover {
      case ex: Exception => {
        log.warn("healthCheck fail: " + ex.getMessage)
        -1
      }
    }
  }

  def listAll: Future[Seq[ScopeInfo]] = {
    db.run(scopeInfos.result)
  }

  def createTable: Future[String] = {
    val schema = scopeInfos.schema
    db.run(DBIO.seq(
      MTable.getTables map (tables => {
        if (!tables.exists(_.name.name == scopeInfos.baseTableRow.tableName))
          db.run(schema.create)
      }))).map(res => "scope info table successfully added").recover {
      case ex: Exception => ex.getCause.getMessage
    }
  }
}