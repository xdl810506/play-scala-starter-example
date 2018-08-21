/*
 * Handlers.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package subsystems.brep

import java.sql.Timestamp

import akka.pattern.pipe
import com.qunhe.diybe.module.parametric.engine.{ParamScriptExecutor, ParamScriptResult}
import com.qunhe.diybe.utils.brep.topo.Shell
import com.qunhe.diybe.utils.brep.utils.BrepDataBuilder
import com.qunhe.log.QHLogger
import mongo.models.ModelData
import paramscript.helper.{ModelParamDataBuilder, ParamScriptHelper}
import play.api.libs.json._
import play.{Boot, Contexts}
import services.api.v1.BrepController
import shared.{Decorating, Supervised}
import slick.models.{GeomodelInfo, ScripttemplateInfo}

import scala.collection.JavaConverters._
import scala.language.postfixOps

case class CREATE_PARAM_MODEL(json: JsValue)

/**
  * - /system/v1 HTTP/REST Brep API back-end handlers
  */
class AbstractHandlers extends Supervised with Decorating {

  override def factory = context.props.deploy.config

  lazy val LOG: QHLogger = QHLogger.getLogger(classOf[BrepController])

  implicit val ec = Contexts.dbWriteOperations

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

      result.pipeTo(sender)
    }
  }
}

class Handlers extends AbstractHandlers