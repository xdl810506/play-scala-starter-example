/*
 * MongoGeoParamScriptDataCollection.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript

import com.mongodb.DB
import com.qunhe.utils.mongo.MongoCollection
import org.mongojack.DBQuery
import org.mongojack.internal.MongoJackModule

/**
  * @author jiangliu
  *
  */
class MongoGeoParamScriptDataCollection(db: DB) extends
  MongoCollection[GeoParamScriptData, String](db, "GeoParamScriptData",
    classOf[GeoParamScriptData], classOf[String]) {

  def get(id: String): GeoParamScriptData = findOne(DBQuery.is("_id", id))

  def upsert(data: GeoParamScriptData): Unit = {
    MongoJackModule.configure(MongoCollection.MAPPER)
    if (data.id != null) {
      if (upsert(DBQuery.is("_id", data.id), data)) {
      }
      else {
        MongoCollection.LOG.message("upsert - update failed").`with`("id", data.id).error()
      }
    } else {
      MongoCollection.LOG.message("upsert - can't find id of geoParamScriptData").error()
    }
  }

  def remove(id: String): Unit = {
    remove(DBQuery.is("_id", id))
  }
}
