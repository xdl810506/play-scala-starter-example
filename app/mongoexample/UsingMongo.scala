/*
 * UsingMongo.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package mongoexample

import com.mongodb.casbah.Imports.{MongoClientOptions, MongoDB, ReadPreference, ServerAddress}
import com.mongodb.casbah.MongoClient

/**
  * Created by jiangliu on 12/08/2018.
  */
object UsingMongo {
  lazy val endpoints = "127.0.0.1:27017"
  lazy val connections = 32

  val mongo: MongoClient = {
    val addresses = endpoints split (",") flatMap {
      case tag => try {
        val tokens = tag split (":")

        Some(new ServerAddress(tokens(0), tokens(1) toInt))
      }
      catch {
        case _: Throwable => None
      }
    } toList

    if (addresses.size > 1) {
      MongoClient(addresses, MongoClientOptions(connectionsPerHost = connections,
        readPreference = ReadPreference.primaryPreferred))
    } else {
      MongoClient(addresses.head, MongoClientOptions(connectionsPerHost = connections))
    }
  }

  def getMongoClient: MongoClient = {
    return mongo
  }

  implicit def apply(db: String): MongoDB = mongo(db)
}
