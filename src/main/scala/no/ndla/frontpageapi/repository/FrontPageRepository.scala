/*
 * Part of NDLA frontpage_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.repository

import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax._
import no.ndla.frontpageapi.integration.DataSource
import no.ndla.frontpageapi.model.domain.FrontPageData
import org.postgresql.util.PGobject
import scalikejdbc._
import FrontPageData._

import scala.util.{Failure, Success, Try}

trait FrontPageRepository {
  this: DataSource =>
  val frontPageRepository: FrontPageRepository

  class FrontPageRepository extends LazyLogging {
    def newFrontPage(page: FrontPageData)(implicit session: DBSession = AutoSession): Try[FrontPageData] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(page.asJson.noSpacesDropNull)

      Try(
        sql"insert into ${FrontPageData.table} (document) values (${dataObject})"
          .updateAndReturnGeneratedKey()
          .apply
      ).map(deleteAllBut).map(_ => page)
    }

    private def deleteAllBut(id: Long)(implicit session: DBSession = AutoSession) = {
      Try(
        sql"delete from ${FrontPageData.table} where id<>${id} "
          .update()
          .apply
      ).map(_ => id)
    }

    def get(implicit session: DBSession = ReadOnlyAutoSession): Option[FrontPageData] = {
      val fr = FrontPageData.syntax("fr")

      sql"select ${fr.result.*} from ${FrontPageData.as(fr)} order by fr.id desc limit 1"
        .map(FrontPageData.fromDb(fr))
        .single
        .apply() match {
        case Some(Success(s)) => Some(s)
        case Some(Failure(ex)) =>
          ex.printStackTrace()
          None
        case None => None
      }
    }

  }
}
