/*
 * MongoGeoParamScriptDataCollection.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript.mongo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.mongodb.DB
import com.qunhe.log.Log
import com.qunhe.utils.mongo.MongoCollection
import org.mongojack.DBQuery
import org.mongojack.internal.MongoJackModule
import paramscript.data.GeoParamScriptTemplateData

/**
  * @author jiangliu
  *
  */
class MongoGeoParamScriptDataCollection(db: DB) extends
  MongoCollection[GeoParamScriptTemplateData, String](db, "GeoParamScriptData",
    classOf[GeoParamScriptTemplateData], classOf[String]) {

  def get(id: String): GeoParamScriptTemplateData = findOne(DBQuery.is("_id", id))

  def upsert(data: GeoParamScriptTemplateData): Unit = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    MongoJackModule.configure(mapper)
    if (data.id != null) {
      if (upsert(DBQuery.is("_id", data.id), data)) {
      }
      else {
        val log: Log[_] = MongoCollection.LOG.message("upsert - update failed")
          .`with`("id", data.id).asInstanceOf[Log[_]]
        log.error()
      }
    } else {
      MongoCollection.LOG.message("upsert - can't find id of geoParamScriptData").error()
    }
  }

  def remove(id: String): Unit = {
    remove(DBQuery.is("_id", id))
  }
}
