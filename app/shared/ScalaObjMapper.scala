/*
 * ScalaObjMapper.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package shared

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import play.api.inject.Binding

import scala.reflect.ClassTag

/**
  * @author qingliang
  *
  */
trait ScalaObjMapper extends Decorating {
  private val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  private lazy val LOG: QHLogger = QHLogger.getLogger(classOf[ScalaObjMapper])

  def writeValueAsString(value: Any): Option[String] = {
    try {
      Some(mapper.writeValueAsString(value))
    } catch {
      case e: Exception => {
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "failed to map object: " + value.toString + " to string, due to " + clarify(e))
        None
      }
    }
  }

  def readValue[T: Manifest](content: String): Option[T] = {
    try {
      Some(mapper.readValue[T](content))
    } catch {
      case e: Exception => {
        LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", "failed to map string: " + content + " to object: " + ", due to " + clarify(e))
        None
      }
    }
  }
}
