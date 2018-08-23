/*
 * Handlers.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package subsystems.brep

import java.sql.Timestamp
import java.util.UUID

import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.qunhe.diybe.module.parametric.engine.ParamScriptResult
import com.qunhe.diybe.utils.brep.topo.Shell
import com.qunhe.diybe.utils.brep.utils.BrepDataBuilder
import com.qunhe.log.{NoticeType, QHLogger, WarningLevel}
import mongo.data.{ModelData, ModelParamTemplateData}
import paramscript.actor._
import paramscript.helper.{ModelParamDataBuilder, ParamScriptHelper}
import play.{Boot, Contexts}
import play.api.libs.concurrent.Futures
import play.api.libs.json._
import services.api.v1.BrepController
import shared.{Decorating, Supervised}
import slick.data.{GeomodelInfo, ScripttemplateInfo}
import statistics.Alert

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

case class CREATE_PARAM_MODEL(json: JsValue)

case class UPDATE_PARAM_MODEL(json: JsValue, shellId: String, scriptTemplateId: String, modelParamTemplateData: ModelParamTemplateData)

case class GET_PARAM_MODEL(shellId: String)

/**
  * - /system/v1 HTTP/REST Brep API back-end handlers
  */
class AbstractHandlers extends Supervised with Decorating {

  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[BrepController])

  lazy val timeoutThreshold: Long = Boot.configuration.getOptional[Long]("qunhe.geoparamengine.http" +
    ".timeout").getOrElse(30000)
  implicit val timeout: Timeout = timeoutThreshold.milliseconds

  implicit val futures = Futures.actorSystemToFutures(Boot.actorSystem)

  object ModelType extends Enumeration {
    val SHELL = "shell"
  }

  override
  def receive = {
    case (CREATE_PARAM_MODEL(json)) => {
      implicit val ec = Contexts.dbWriteOperations
      val scriptData = ParamScriptHelper.buildParamScriptDataFromJson(json)
      val scriptExecutorActor = named(new ScriptExecutorActor, UUID.randomUUID.toString())

      val senderRef = sender
      (scriptExecutorActor ? EXECUTE_SCRIPT(scriptData)).mapTo[Option[ParamScriptResult]].map(paramScriptResOpt => {
        paramScriptResOpt match {
          case Some(resultParam) => {
            try {
              val output: String = scriptData.savedOutputIds.headOption.getOrElse("")
              val shell: Shell = resultParam.getResultMap.get(output).asInstanceOf[Shell]
              val shellName = shell.getName

              val (modelParamTemplateData, modelParamData) = ModelParamDataBuilder.
                buildModelParamAndTemplateData(shellName, scriptData)

              val shells: List[Shell] = List(shell)
              val modelData = BrepDataBuilder.toJson(BrepDataBuilder.buildToDatas(shells.asJava, null))


              // add related data into mongodb
              val addModelDataRes = Boot.modelDataRepo.addModelData(ModelData(shellName, shellName, Json.parse(modelData)))
              val addModelParamDataRes = Boot.modelParamDataRepo.addModelParmData(modelParamData)
              val addModelParamTemplateDataRes = Boot.modelParamTemplateDataRepo.addModelParmTemplateData(modelParamTemplateData)

              // add related metadata into mysql
              val currentTime = new Timestamp(System.currentTimeMillis())
              val scripttemplateInfo = ScripttemplateInfo(None, "LinearExtrusionFace", modelParamTemplateData._id, currentTime, currentTime)
              val addGeoModelInfoAndScriptTemplateRes = Boot.scriptTemplateRepo.add(scripttemplateInfo)
                .flatMap({ paramtemplateid => {
                  val geomodelInfo = GeomodelInfo(None, shellName, ModelType.SHELL, paramtemplateid, modelParamTemplateData._id, currentTime, currentTime)
                  Boot.geoModelRepo.add(geomodelInfo)
                }
                })

              // run futures in parallel
              val result = for {
                r1 <- addModelDataRes
                r2 <- addModelParamDataRes
                r3 <- addModelParamTemplateDataRes
                r4 <- addGeoModelInfoAndScriptTemplateRes
              } yield (Map(
                "outcome" -> "new shell being created",
                "shell id" -> shellName
              ))

              result.pipeTo(senderRef)
            } catch {
              case e: Exception => {
                val errorMsg = clarify(e)
                Alert(self, "failed to create shell due to " + errorMsg)
                Future(Map(
                  "outcome" -> "failed to create shell",
                  "diagnostics" -> errorMsg
                )).pipeTo(senderRef)
              }
            }
          }
          case _ => {
            Future(Map(
              "outcome" -> "failed to execute parametric script"
            )).pipeTo(senderRef)
          }
        }
      })(Contexts.expensiveCpuOperations).recover {
        case e: scala.concurrent.TimeoutException => {
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", "execute parametric script timeout after "
            + timeoutThreshold + " milliseconds")
          Future(Map(
            "outcome" -> "execute parametric script timeout"
          )).pipeTo(senderRef)
        }
        case e: Exception => {
          val errorMsg = "failed to execute parametric script due to: " + clarify(e)
          LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", errorMsg)
          Alert(self, errorMsg)
          Future(Map(
            "outcome" -> ("server error: " + e.getMessage)
          )).pipeTo(senderRef)
        }
      }
    }
    case (UPDATE_PARAM_MODEL(json, shellId, scriptTemplateId, modelParamTemplateData)) => {
      implicit val ec = Contexts.dbWriteOperations

      val scriptData = modelParamTemplateData.parmScript
      val userInputs = ParamScriptHelper.buildUserInputsFromJson(json)

      val scriptExecutorActor = named(new ScriptExecutorActor, UUID.randomUUID.toString())
      scriptExecutorActor ! EXECUTE_SCRIPT_WITH_USERINPUTS(scriptData, userInputs)

      val senderRef = sender
      (scriptExecutorActor ? EXECUTE_SCRIPT_WITH_USERINPUTS(scriptData, userInputs)).mapTo[Option[ParamScriptResult]].map(paramScriptResOpt => {
        paramScriptResOpt match {
          case Some(resultParam) => {
            try {
              val output: String = scriptData.savedOutputIds.headOption.getOrElse("")
              val shellNew: Shell = resultParam.getResultMap.get(output).asInstanceOf[Shell]

              val shells: List[Shell] = List(shellNew)
              val modelData = BrepDataBuilder.toJson(BrepDataBuilder.buildToDatas(shells.asJava, null))
              val modelDataNew = modelData.replaceAll(shellNew.getName, shellId);

              val modelParamData = ModelParamDataBuilder.
                buildModelParamDataWithUserInputs(shellId, scriptData,
                  userInputs, scriptTemplateId)

              val editModelDataRes = Boot.modelDataRepo.updateModelData(shellId, ModelData(shellId, shellId, Json.parse(modelDataNew)))
              val editModelParamDataRes = Boot.modelParamDataRepo.updateModelParamData(shellId, modelParamData)
              val result = for {
                r1 <- editModelDataRes
                r2 <- editModelParamDataRes
              } yield (Map(
                "outcome" -> "shell being updated",
                "shell id" -> shellId
              ))

              result.pipeTo(senderRef)
            } catch {
              case e: Exception => {
                val errorMsg = clarify(e)
                Alert(self, "failed to update shell " + errorMsg)
                Future(Map(
                  "outcome" -> "failed to update shell",
                  "diagnostics" -> errorMsg
                )).pipeTo(senderRef)
              }
            }
          }
          case _ => {
            Future(Map(
              "outcome" -> "failed to execute parametric script"
            )).pipeTo(senderRef)
          }
        }
      })(Contexts.expensiveCpuOperations).recover {
        case e: scala.concurrent.TimeoutException => {
          LOG.notice(WarningLevel.WARN, NoticeType.WE_CHAT, "Geometry Middleware", "execute parametric script timeout after "
            + timeoutThreshold + " milliseconds")
          Future(Map(
            "outcome" -> "execute parametric script timeout"
          )).pipeTo(senderRef)
        }
        case e: Exception => {
          val errorMsg = "failed to execute parametric script due to: "+ clarify(e)
          LOG.notice(WarningLevel.ERROR, NoticeType.WE_CHAT, "Geometry Middleware", errorMsg)
          Alert(self, errorMsg)
          Future(Map(
            "outcome" -> ("server error: " + e.getMessage)
          )).pipeTo(senderRef)
        }
      }
    }
    case (GET_PARAM_MODEL(shellId)) => {
      implicit val ec = Contexts.expensiveDbLookups
      val getModelDataRes = Boot.modelDataRepo.getModelData(shellId)
      val getGeoModelInfoRes = Boot.geoModelRepo.get(shellId)
      val result = for {
        r1 <- getModelDataRes
        r2 <- getGeoModelInfoRes
      } yield ((r1, r2) match {
        case (Some(shelldata), Some(geomodelInfo)) => {
          Some(Map(
            "outcome" -> "shell data found",
            "template script id" -> geomodelInfo.paramtemplatedataid,
            "shell data" -> Json.parse(shelldata.modelData.toString)
          ))
        }
        case (_, _) => {
          None
        }
      })

      result.pipeTo(sender)
    }
  }
}

class Handlers extends AbstractHandlers