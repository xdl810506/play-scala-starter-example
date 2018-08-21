/*
 * Handlers.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package subsystems.brep

import java.sql.Timestamp

import com.qunhe.diybe.module.parametric.engine.{ParamScriptExecutor, ParamScriptResult}
import com.qunhe.diybe.utils.brep.topo.Shell
import com.qunhe.diybe.utils.brep.utils.BrepDataBuilder
import com.qunhe.log.QHLogger
import mongo.models.ModelData
import paramscript.helper.{ModelParamDataBuilder, ParamScriptHelper}
import play.Boot
import play.api.libs.json._
import services.api.v1.BrepController
import shared.{Decorating, Supervised}
import slick.models.{GeomodelInfo, ScripttemplateInfo}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

case class CREATE_PARAM_MODEL(json: JsValue)

/**
  * - /system/v1 HTTP/REST Brep API back-end handlers
  */
class AbstractHandlers extends Supervised with Decorating {

  override def factory = context.props.deploy.config

  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[BrepController])

  object ModelType extends Enumeration {
    val SHELL = "shell"
  }

  override
  def receive = {
    case (CREATE_PARAM_MODEL(json)) => {
      val scriptData = ParamScriptHelper.buildParamScriptDataFromJson(json)
      val executor: ParamScriptExecutor = ParamScriptHelper.paramScriptExecutor

      val resultParam: ParamScriptResult = executor.execute(scriptData.toParamScript)
      val output: String = scriptData.savedOutputIds.headOption.getOrElse("")
      val shell: Shell = resultParam.getResultMap.get(output).asInstanceOf[Shell]
      val shellName = shell.getName

      val (modelParamTemplateData, modelParamData) = ModelParamDataBuilder.
        buildModelParamAndTemplateData(shellName, scriptData)

      val shells: List[Shell] = List(shell)
      val modelData = BrepDataBuilder.toJson(BrepDataBuilder.buildToDatas(shells.asJava, null))
      val addModelDataRes = Boot.modelDataRepo.addModelData(ModelData(shellName, shellName, Json.parse(modelData)))
      val addModelParamDataRes = Boot.modelParamDataRepo.addModelParmData(modelParamData)
      val addModelParamTemplateDataRes = Boot.modelParamTemplateDataRepo.addModelParmTemplateData(modelParamTemplateData)
      val result = for {
        r1 <- addModelDataRes
        r2 <- addModelParamDataRes
        r3 <- addModelParamTemplateDataRes
      } yield (Map(
        "outcome" -> "new shell being created",
        "shell id" -> shellName
      ))

      val currentTime = new Timestamp(System.currentTimeMillis())
      Boot.scriptTemplateRepo.add(ScripttemplateInfo(None, "LinearExtrusionFace", modelParamTemplateData._id, currentTime, currentTime)).map({
        paramtemplateid => {
          Boot.geoModelRepo.add(GeomodelInfo(None, shellName, ModelType.SHELL, paramtemplateid, modelParamTemplateData._id, currentTime, currentTime))
        }
      })

      result.map({
        outcome => sender ! outcome
      }).recover {
        case e: Throwable => {
          val outcome = Map(
            "outcome" -> ("server error: " + clarify(e))
          )
          sender ! outcome
        }
      }
    }
  }
}

class Handlers extends AbstractHandlers