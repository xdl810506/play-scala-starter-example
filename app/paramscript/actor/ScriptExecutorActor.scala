/*
 * ScriptExecutorActor.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package paramscript.actor

import com.qunhe.diybe.module.parametric.engine.{ParamScriptExecutor, ParamScriptResult}
import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import paramscript.data.ParamScriptData
import paramscript.helper.ParamScriptHelper
import shared.{Decorating, Supervised}

import scala.collection.JavaConverters._
import scala.language.postfixOps

case class EXECUTE_SCRIPT(scriptData: ParamScriptData)

case class EXECUTE_SCRIPT_WITH_USERINPUTS(scriptData: ParamScriptData, userInputs: Map[String, Any])

/**
  * - /system/v1 HTTP/REST Parametric Script API back-end handlers
  */
class ScriptExecutorActor extends Supervised with Decorating {

  override def factory = context.props.deploy.config

  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[ScriptExecutorActor])

  //implicit val ec = Contexts.dbWriteOperations

  override
  def receive = {
    case (EXECUTE_SCRIPT(scriptData: ParamScriptData)) => {
      try {
        val executor: ParamScriptExecutor = ParamScriptHelper.paramScriptExecutor

        val resultParam: ParamScriptResult = executor.execute(scriptData.toParamScript)

        sender ! Some(resultParam)
      } catch {
        case e: Exception => {
          val errorMsg = "parameter script execution failed with " + clarify(e)
          LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", errorMsg)
          sender ! None
        }
      }
    }
    case (EXECUTE_SCRIPT_WITH_USERINPUTS(scriptData: ParamScriptData, userInputs)) => {
      try {
        val executor: ParamScriptExecutor = ParamScriptHelper.paramScriptExecutor

        val resultParam: ParamScriptResult = executor.execute(scriptData.toParamScript, userInputs.asJava)

        sender ! Some(resultParam)
      } catch {
        case e: Exception => {
          val errorMsg = "parameter script execution with user inputs failed with " + clarify(e)
          LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", errorMsg)
          sender ! None
        }
      }
    }
  }
}