/*
 * System.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */
package services.api.v1

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date

import play.api.mvc._

import scala.concurrent.duration.{Duration, _}
import scala.language.postfixOps


trait System {
  this: Controller with Authenticated =>

  lazy val router = Boot.system.actorSelection("akka://application/user/daemons/router")

  lazy val dataHandler = Boot.system.actorSelection("akka://application/user/daemons/router/data")

  lazy val scopeinfo = Boot.scopeRepository

  lazy val logsTable = Boot.logsRepository

  lazy val style = defaulted(config getString ("platform.compute.application.time.style"), "dd-MMM-yyyy HH:mm:ss")

  lazy val formatter = new SimpleDateFormat(style)


  case class DASHBOARD(window: Duration)

  case class DEPENDENCIES(window: Duration)

  /**
    * - our API tag (used to scribe stuff)
    */
  lazy val tag = "/"

  def dashboard = verify(tag,
    {
      case (sender, verified) => {
        val routed = (new DASHBOARD(1 minutes), Routed(sender, "system", verified))
        router ! routed
      }
      //case (sender, verified) => Router(DASHBOARD(5 minutes), sender, "system", verified)
    })(30 seconds, true)

  def dependencies = verify(tag, {
    case (sender, verified) => {
      val routed = (new DEPENDENCIES(5 minutes), Routed(sender, "system", verified))
      router ! routed
    }
  })(30 seconds, true)

  def aurora = Action.async {
    implicit request => {
      Boot.scopeRepository.listAll map {
        logs => Ok("logs size is " + logs.size.toString)
      }
    }
  }

  def expiredtimer = Action {
    request => {
      try {
        log debug ("request expired timer.")
        val when = new Date()

        val formatted = formatter format (when)
        val now = java.lang.System.currentTimeMillis
        val expires_at = new Timestamp(new Date(now + ((1 day).toMillis)).getTime())

        scopeinfo.getExpiredscopes(new Timestamp(now)) map {
          slist =>
            slist map {
              s => {
                val scope = s.scope
                val urn = s.alias
                val endpoint = s.callback_bubble
                val completedSuccessfully = s.completed
                val requested = s.requested
                val percentSuccess = if (requested <= 0) 0 else (100 * (completedSuccessfully)) / requested
                dataHandler ! TimeOutMSG(scope, urn, endpoint, percentSuccess)
              }
            }
        } recover {
          case ex: Exception => {
            log.warn("sent timeout message exception " + ex.toString)
          }
        }

        scopeinfo.expiredScopes(new Timestamp(when.getTime), expires_at, new Timestamp(now), new Timestamp(now))
        Ok("set timed-out entries as inactive")
      }
      catch {
        case e: Throwable => {
          log.warn("unable to query for time-outs (database failure) exception: " + e)
          InternalServerError("unable to query for time-outs (database failure) exception: " + e)
        }
      }
    }
  }
}

object System extends Controller with System with Authenticated
