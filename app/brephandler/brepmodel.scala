/*
 * brepmodel.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package brephandler

import java.util.UUID

import com.qunhe.diybe.module.math2.base.{CCS, Point2d}
import com.qunhe.diybe.module.math2.geom2d.LineSeg2d
import com.qunhe.diybe.module.parametric.engine.annotation.ParamFunction
import com.qunhe.diybe.utils.brep.exceptions.ValidationException
import com.qunhe.diybe.utils.brep.topo.Shell
import com.qunhe.diybe.utils.brep.topo.nm.ShellId
import com.qunhe.diybe.utils.brep.utils.{BrepBuilder, BrepDataBuilder}

import scala.collection.JavaConverters._

/**
  * @author jiangliu
  *
  */
@ParamFunction.Package("BrepHandler")
object brepmodel {

  /**
    * get the texturePosition and build the tileComponent
    *
    * @param startPt
    * @param endPt
    * @param height
    * @return Face
    */
  @ParamFunction("createFaceByLinearExtrusion")
  def createFaceByLinearExtrusion(
    @ParamFunction.Param("startPt") startPt: Point2d,
    @ParamFunction.Param("endPt") endPt: Point2d,
    @ParamFunction.Param("height") height: Double): (String, String) = {
    val curve2d = new LineSeg2d(startPt, endPt)
    val ccs = new CCS()
    try {
      val shell: Shell = new Shell(BrepBuilder.createFaceByLinearExtrusion(curve2d, ccs, height))
      shell.setId(new ShellId(UUID.randomUUID.toString()))
      val shells: List[Shell] = List(shell)
      val modelString = BrepDataBuilder.toJson(BrepDataBuilder.buildToDatas(shells.asJava, null))
      (shell.getName, modelString)
    } catch {
      case e: ValidationException => {
        e.printStackTrace()
        ("", "")
      }
    }
  }
}
