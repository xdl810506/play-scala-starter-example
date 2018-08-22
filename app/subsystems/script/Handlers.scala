/*
 * Handlers.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package subsystems.script

import akka.pattern.pipe
import play.{Boot, Contexts}
import play.api.libs.json._
import shared.{ScalaObjMapper, Supervised}

import scala.language.postfixOps

case class GET_TEMPLATE_SCRIPT(templateScriptId: String)

/**
  * - /system/v1 HTTP/REST Parametric Script API back-end handlers
  */
class AbstractHandlers extends Supervised with ScalaObjMapper {

  override def factory = context.props.deploy.config

  implicit val ec = Contexts.expensiveDbLookups

  override
  def receive = {
    case (GET_TEMPLATE_SCRIPT(templateScriptId)) => {
      val result = Boot.modelParamTemplateDataRepo.getModelParamTemplateData(templateScriptId).map(scriptTemplateDataOpt => {
        scriptTemplateDataOpt match {
          case Some(scriptTemplateData) => {
            writeValueAsString(scriptTemplateData) match {
              case Some(scriptData) => Some(Map(
                "outcome" -> "template script data found",
                "template script data" -> Json.parse(scriptData)
              ))
              case _ => None
            }
          }
          case _ => None
        }
      })

      result.pipeTo(sender)
    }
  }
}

class Handlers extends AbstractHandlers