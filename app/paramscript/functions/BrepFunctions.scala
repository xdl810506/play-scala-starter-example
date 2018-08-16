/*
 * brepmodel.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package paramscript.functions

import java.util.UUID

import com.qunhe.diybe.module.math2.base.{CCS, Point2d}
import com.qunhe.diybe.module.math2.geom2d.LineSeg2d
import com.qunhe.diybe.module.parametric.engine.annotation.ParamFunction
import com.qunhe.diybe.utils.brep.exceptions.ValidationException
import com.qunhe.diybe.utils.brep.topo.Shell
import com.qunhe.diybe.utils.brep.topo.nm.ShellId
import com.qunhe.diybe.utils.brep.utils.BrepBuilder

/**
  * @author jiangliu
  *
  */
@ParamFunction.Package("BrepModeling")
class BrepFunctions {

  /**
    * get the start point, end point and extrusion height, and build the Face
    *
    * @param startPt
    * @param endPt
    * @param height
    * @return Shell
    */
  @ParamFunction("createFaceByLinearExtrusion")
  def createFaceByLinearExtrusion(
    @ParamFunction.Param("startPt") startPt: Point2d,
    @ParamFunction.Param("endPt") endPt: Point2d,
    @ParamFunction.Param("height") height: Double): Shell = {
    val curve2d = new LineSeg2d(startPt, endPt)
    val ccs = new CCS()
    try {
      val shell: Shell = new Shell(BrepBuilder.createFaceByLinearExtrusion(curve2d, ccs, height))
      shell.setId(new ShellId(UUID.randomUUID.toString()))
      shell
    } catch {
      case e: ValidationException => {
        e.printStackTrace()
        null
      }
    }
  }
}
