/*
 * ScopeController.scala
 * Copyright 2018 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package controllers

import java.util.UUID

import akka.actor.ActorSystem
import javax.inject._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._
import slickexample.dal.ScopeRepository
import slickexample.models.ScopeInfo

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future, Promise}

class ScopeController @Inject()(repo: ScopeRepository,
                                cc: ControllerComponents,
                                actorSystem: ActorSystem
                               )(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  /**
    * The mapping for the scope form.
    */
  val scopeForm: Form[CreateScopeForm] = Form {
    mapping(
      "scope" -> nonEmptyText,
      "family" -> nonEmptyText
    )(CreateScopeForm.apply)(CreateScopeForm.unapply)
  }

  /**
    * The add person action.
    *
    * This is asynchronous, since we're invoking the asynchronous methods on PersonRepository.
    */
  def addScope = {
    Action.async {
      lazy val scope = UUID.randomUUID.toString()
      repo.add(ScopeInfo(None, scope, "test"))
      getFutureMessage(1.second).map { msg => Ok(msg) }
    }
  }

  /**
    * A REST endpoint that gets all the people as JSON.
    */
  def getScopes = {
    Action.async {
      repo.listAll.map { scopeInfos =>
        Ok(Json.toJson(scopeInfos))
      }

      //getFutureMessage(1.second).map { msg => Ok(msg) }
    }
  }

  private
  def getFutureMessage(delayTime: FiniteDuration): Future[String] = {
    val promise: Promise[String] = Promise[String]()
    actorSystem.scheduler.scheduleOnce(delayTime) {
      promise.success("Hi!")
    }(actorSystem.dispatcher) // run scheduled tasks using the actor system's dispatcher
    promise.future
  }
}

/**
  * The create scope form.
  *
  * Generally for forms, you should define separate objects to your models, since forms very often need to present data
  * in a different way to your models.  In this case, it doesn't make sense to have an id parameter in the form, since
  * that is generated once it's created.
  */
case class CreateScopeForm(scope: String, family: String)
