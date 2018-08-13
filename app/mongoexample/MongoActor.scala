/*
 * MongoActor.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package mongoexample

import akka.actor.{Actor, Cancellable}
import com.mongodb.WriteConcern
import com.mongodb.casbah.commons.MongoDBObject


/**
  * Created by jiangliu on 08/12/2018.
  */

case
class ADDPARAMMODEL(modelId: String, modelData: String)

/*case class ADD(scope: String)

case class UNLOCK(scope: String)

case class ACK(scope: String, log: List[String], ir: Int)*/


class MongoActor extends Actor {
  lazy val models = UsingMongo("parammodel")("model")
  var timer: Option[Cancellable] = None

  override
  def receive: Receive = {
    case ADDPARAMMODEL(modelId, modelData) => {
      println("add " + modelId)
      val dbo = MongoDBObject(
        "_id" -> modelId,
        "modelId" -> modelId,
        "modelData" -> modelData
      )
      models insert(dbo, WriteConcern.ACKNOWLEDGED)
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
