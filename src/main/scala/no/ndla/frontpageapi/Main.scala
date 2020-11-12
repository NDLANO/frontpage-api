/*
 * Part of NDLA frontpage_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import no.ndla.frontpageapi.FrontpageApiProperties.ApplicationPort
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.log4s.getLogger

import scala.io.Source
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.language.higherKinds

object Main extends IOApp {
  val logger = getLogger

  override def run(args: List[String]): IO[ExitCode] = {
    val envMap = System.getenv()
    envMap.asScala.foreach { case (k, v) => System.setProperty(k, v) }

    logger.info(
      Source
        .fromInputStream(getClass.getResourceAsStream("/log-license.txt"))
        .mkString)

    logger.info("Starting database migration")
    DBMigrator.migrate(ComponentRegistry.dataSource)

    logger.info("Building swagger service")
    val routes = Routes.buildRoutes()

    logger.info(s"Starting on port $ApplicationPort")
    val app = Router[IO](
      routes.map(r => r.mountPoint -> r.toRoutes): _*
    ).orNotFound

    BlazeServerBuilder[IO]
      .withHttpApp(app)
      .bindHttp(ApplicationPort, "0.0.0.0")
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

  }
}
