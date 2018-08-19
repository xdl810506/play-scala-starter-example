import play.api.http.HttpErrorHandler
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent._

/*
 * CustomHttpErrorHandler.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

/**
  * @author jiangliu
  *
  */
// https://www.playframework.com/documentation/2.5.x/ScalaErrorHandling
// https://www.programcreek.com/scala/play.api.http.httperrorhandler
class ErrorHandler extends HttpErrorHandler {
  private def jsonResponse(statusCode: Int, message: String): JsObject =
    Json.obj(
      "errors" -> Json.arr(
        Json.obj(
          "status" -> statusCode,
          "detail" -> message
        )
      )
    )

  def onClientError(
                     request: RequestHeader,
                     statusCode: Int,
                     message: String
                   ): Future[Result] = Future.successful {
    Status(statusCode)(jsonResponse(statusCode, message))
  }

  def onServerError(
                     request: RequestHeader,
                     exception: Throwable
                   ): Future[Result] = Future.successful {
    InternalServerError(jsonResponse(500, "Internal server error"))
  }
}
