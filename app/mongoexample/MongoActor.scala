/*
 * MongoActor.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package mongoexample

import java.io.{PrintWriter, StringWriter}

import akka.actor.Actor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.util.JSON
import com.mongodb.{DBObject, WriteConcern}
import com.qunhe.diybe.utils.brep.topo.Shell
import com.qunhe.diybe.utils.brep.utils.BrepDataBuilder
import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import paramscript.data.{GeoParamScriptData, GeoParamScriptTemplateData}
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.util.control.NonFatal


/**
  * Created by jiangliu on 08/12/2018.
  */

case
class ADD_PARAM_MODEL(shell: Shell)

case
class ADD_PARAM_TEMPLATE_SCRIPT(shellId: String, scriptTemplateData: GeoParamScriptTemplateData)

case class ADD_PARAM_SCRIPT_DATA(shellId: String, scriptData: GeoParamScriptData)

case class GET_PARAM_MODEL(shellId: String)

case class GET_PARAM_SCRIPT_DATA(shellId: String)

case class GET_PARAM_TEMPLATE_SCRIPT(scriptTemplateId: String)

case class EDIT_PARAM_MODEL(shellId: String, shellNew: Shell)

case class EDIT_PARAM_SCRIPT_DATA(shellId: String, scriptData: GeoParamScriptData)

/*case class ADD(scope: String)

case class UNLOCK(scope: String)

case class ACK(scope: String, log: List[String], ir: Int)*/


class MongoActor extends Actor {
  lazy val models = UsingMongo("parammodel")("model")
  lazy val templateScripts = UsingMongo("parammodel")("GeoParamScriptTemplateData")
  lazy val scripts = UsingMongo("parammodel")("GeoParamScriptData")
  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[MongoActor])

  /*import com.mongodb.DB

  lazy val db: DB = UsingMongo.getMongoClient.getDB("parammodel").underlying
  lazy val geoParamScriptDataColl = new MongoGeoParamScriptDataCollection(db)*/

  override
  def receive: Receive = {
    case ADD_PARAM_MODEL(shell) => {
      val modelId = shell.getName
      LOG.message("add " + modelId)

      val shells: List[Shell] = List(shell)
      val modelData = BrepDataBuilder.toJson(BrepDataBuilder.buildToDatas(shells.asJava, null))
      val dbObject: DBObject = JSON.parse(modelData).asInstanceOf[DBObject]
      val dbo = MongoDBObject(
        "_id" -> modelId,
        "modelId" -> modelId,
        "modelData" -> dbObject
      )
      try {
        models insert(dbo, WriteConcern.ACKNOWLEDGED)
      }
      catch {
        case NonFatal(e) => {
          val sw = new StringWriter
          e.printStackTrace(new PrintWriter(sw))
          LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
            "to add shell data to mongodb for " + modelId)
        }
      }
    }
    case EDIT_PARAM_MODEL(shellId, shellNew) => {
      LOG.message("edit " + shellId)

      val shells: List[Shell] = List(shellNew)
      val newShellId = shellNew.getName
      val modelData = BrepDataBuilder.toJson(BrepDataBuilder.buildToDatas(shells.asJava, null))
      val modelDataNew = modelData.replaceAll(newShellId, shellId);
      val dbObject: DBObject = JSON.parse(modelDataNew).asInstanceOf[DBObject]
      val dbo = MongoDBObject(
        "_id" -> shellId,
        "modelId" -> shellId,
        "modelData" -> dbObject
      )
      val query = MongoDBObject("_id" -> shellId)
      try {
        models update(query, dbo, false, false, WriteConcern.ACKNOWLEDGED)
      }
      catch {
        case NonFatal(e) => {
          val sw = new StringWriter
          e.printStackTrace(new PrintWriter(sw))
          LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
            "to update shell data to mongodb for " + shellId)
        }
      }
    }
    case ADD_PARAM_TEMPLATE_SCRIPT(shellId, scriptTemplateData) => {
      LOG.message("add param template script for " + shellId)
      val mapper = new ObjectMapper() with ScalaObjectMapper
      mapper.registerModule(DefaultScalaModule)
      val scriptTemplateJson = mapper.writeValueAsString(scriptTemplateData)

      val dbObject: DBObject = JSON.parse(scriptTemplateJson).asInstanceOf[DBObject]
      try {
        templateScripts insert(dbObject, WriteConcern.ACKNOWLEDGED)
      }
      catch {
        case NonFatal(e) => {
          val sw = new StringWriter
          e.printStackTrace(new PrintWriter(sw))
          LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
            "to add param template script to mongodb for " + shellId)
        }
      }
    }
    case ADD_PARAM_SCRIPT_DATA(shellId, scriptData) => {
      LOG.message("add param script data for " + shellId)
      val mapper = new ObjectMapper() with ScalaObjectMapper
      mapper.registerModule(DefaultScalaModule)
      val scriptTemplateJson = mapper.writeValueAsString(scriptData)

      val dbObject: DBObject = JSON.parse(scriptTemplateJson).asInstanceOf[DBObject]
      try {
        scripts insert(dbObject, WriteConcern.ACKNOWLEDGED)
      }
      catch {
        case NonFatal(e) => {
          val sw = new StringWriter
          e.printStackTrace(new PrintWriter(sw))
          LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
            "to add param script data to mongodb for " + shellId)
        }
      }
    }
    case EDIT_PARAM_SCRIPT_DATA(shellId, scriptData) => {
      LOG.message("edit param script data for " + shellId)
      val mapper = new ObjectMapper() with ScalaObjectMapper
      mapper.registerModule(DefaultScalaModule)
      val scriptTemplateJson = mapper.writeValueAsString(scriptData)

      val dbObject: DBObject = JSON.parse(scriptTemplateJson).asInstanceOf[DBObject]
      val query = MongoDBObject("_id" -> shellId)
      try {
        scripts update(query, dbObject, false, false, WriteConcern.ACKNOWLEDGED)
      } catch {
        case NonFatal(e) => {
          val sw = new StringWriter
          e.printStackTrace(new PrintWriter(sw))
          LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "Failed " +
            "to update param script data to mongodb")
        }
      }
    }
    case GET_PARAM_MODEL(shellId) => {
      LOG.message("get shell data for " + shellId)
      val filter = MongoDBObject("_id" -> 1, "modelId" -> 1, "modelData" -> 1)
      val query = MongoDBObject("_id" -> shellId)
      models findOne(query, filter) match {
        case Some(dbo) => {
          val fields = dbo toMap

          fields get ("modelData") toString match {
            case modelData: String => {
              sender ! Some(modelData)
            }
            case _ => {
              LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", "shell " +
                "data not being able to interpret as string for " + shellId)
              sender ! None
            }
          }
        }
        case _ => {
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", "no shell data" +
            " return from mongo query for " + shellId)
          sender ! None
        }
      }
    }
    case GET_PARAM_SCRIPT_DATA(shellId) => {
      LOG.message("get shell param script data for " + shellId)
      val filter = MongoDBObject("_id" -> 1, "paramScriptRefData" -> 1)
      val query = MongoDBObject("_id" -> "test")
      scripts findOne(query, filter) match {
        case Some(dbo) => {
          val fields = dbo toMap

          fields get ("paramScriptRefData") toString match {
            case paramRefData: String => {
              val templateId: String = (Json.parse(paramRefData) \ "scriptTemplateId").as[String]
              sender ! Some(templateId)
            }
            case _ => {
              LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", "shell " +
                "param script data not being able to interpret as string for " + shellId)
              sender ! None
            }
          }
        }
        case _ => {
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", "no shell " +
            "param script data return from mongo query for " + shellId)
          sender ! None
        }
      }
    }
    case GET_PARAM_TEMPLATE_SCRIPT(scriptTemplateId) => {
      LOG.message("get template param script data for " + scriptTemplateId)
      val filter = MongoDBObject("_id" -> 1, "script" -> 1)
      val query = MongoDBObject("_id" -> scriptTemplateId)
      templateScripts findOne(query, filter) match {
        case Some(dbo) => {
          val fields = dbo toMap

          fields get ("script") toString match {
            case scriptData: String => {
              sender ! Some(scriptData)
            }
            case _ => {
              LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware",
                "template param script data not being able to interpret as string for " +
                  scriptTemplateId)
              sender ! None
            }
          }
        }
        case _ => {
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware",
            "no template param script data" +
              " return from mongo query for " + scriptTemplateId)
          sender ! None
        }
      }
    }
    /*case ADD(scope)                      => {
      println("add")
      val dbo = MongoDBObject(
        "scope" -> scope,
        "family" -> "TS_LoadTest_10",
        "alias" -> "urn:adsk.s3:derived.file:translation_25_testing/TR-loadtest-929477643943838556",
        "active" -> false,
        "ops" -> MongoDBList(),
        "remaining" -> 10,
        "requested" -> 1,
        "completed" -> 0,
        "started at" -> new Date(),
        "updated at" -> new Date(),
        "expires at" -> new Date(),
        "guid" -> "urn:adsk.s3:derived.file:translation_25_testing/TR-loadtest-929477643943838556",
        "client" -> "",
        "user" -> "",
        "log" -> MongoDBList(),
        "requested as" -> "FDPBRBNRLSAM",
        "jobs" -> MongoDBList(),
        "closed at" -> new Date()
        //        "closed at" -> new Date(),
        //        "locks" -> 0

      )
      logs insert(dbo, WriteConcern.ACKNOWLEDGED)
    }
    case UNLOCK(scope)                   => {
      /**
        * - unlock anyway.
        */
      val query3 = Map("scope" -> scope, "locks" -> 1)
      val update3 = MongoDBObject({
        "$inc" -> MongoDBObject("locks" -> -1)
      })
      logs.findAndModify(query3, update3)
      println(scope + " unlocked.")
    }
    case ACK(scope, toLog, incRemaining) => {

      //      val query = Map("scope" -> scope, "locks" -> 0)
      val query = MongoDBObject("scope" -> scope) ++
        ($or(("locks" -> MongoDBObject("$exists" -> false)), ("locks" -> 0)))

      val update2 = MongoDBObject({
        "$inc" -> MongoDBObject("locks" -> 1)
      })
      //val acd = Some(query)
      logs.findAndModify(query, update2) match {
        case Some(dbo) => try {
          val filter = Map(
            "active" -> 1,
            "family" -> 1,
            "requested as" -> 1,
            "started at" -> 1,
            "remaining" -> 1,
            "completed" -> 1,
            "requested" -> 1,
            "ops" -> 1,
            "requestor" -> 1,
            "alias" -> 1,
            "extractor" -> 1,
            "callback_bubble" -> 1)

          logs findOne(Map("scope" -> scope), filter) match {
            case Some(dbo) => {
              val fields = dbo toMap

              (fields get ("remaining"),
                fields get ("ops"),
                fields get ("requestor")) match {
                case (remaining: Int, ops: BasicDBList, requestor) => {

                  val basic = MongoDBObject(
                    List("remaining" -> (remaining - 1),
                      ("ops.0.status") -> "haha",
                      ("ops.0.updated at") -> new Date()))


                  val dbfields = Map("$set" -> basic) ++ Map("$inc" -> MongoDBObject("locks" -> -1))
                  logs update
                    (Map("scope" -> scope), dbfields, false, false, WriteConcern.ACKNOWLEDGED
                      .withJournal(true))
                }
              }

            }
          }
          println(toLog.toString() + " done")
        } catch {
          case _ => {
            println(scope + " failed, wait for 1 sec and unlock")
            timer = Some(context.system.scheduler.scheduleOnce(1 seconds, self, UNLOCK(scope)))
          }
        }

        case _ => {
          println("error operation!")
        }
      }


      /*
      val query = MongoDBObject({"scope" -> scope})
      val update1 = MongoDBObject({"$pushAll" -> MongoDBObject("log" -> MongoDBList(toLog: _*))})
      val update2 = MongoDBObject({"$inc" -> MongoDBObject("remaining" -> incRemaining)})
      logs.findAndModify(query, update1)

                  /**
                    * - fire the update to mongo
                    */
    //  logs update(Map("scope" -> scope), update1, false, false, WriteConcern.ACKNOWLEDGED.withJournal(true))
      
      logs.findAndModify(query, update2)
      * 
      */
    }*/
    case _ => println("_")
  }
}
