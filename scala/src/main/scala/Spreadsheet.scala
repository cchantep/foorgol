package fr.applicius.foorgol

import java.io.InputStreamReader
import java.util.{ Date, Locale }
import java.text.SimpleDateFormat

import org.apache.http.NameValuePair

import scala.xml.{ Elem, InputSource, Node, XML }
import scala.concurrent.{ ExecutionContext, Future }

import resource.{ ManagedResource, managed }

/** Google spreadsheet DSL. */
trait Spreadsheet { http: WithHttp ⇒
  /** Execution context */
  implicit def context: ExecutionContext

  /** OAuth access token */
  def accessToken: String

  /** Can be used with offline access token, to refresh it. */
  def refreshToken: Option[RefreshToken]

  def list: Future[List[SpreadsheetInfo]] = {
    @annotation.tailrec
    def go(entries: Seq[Node], infos: List[SpreadsheetInfo]): Future[List[SpreadsheetInfo]] = entries.headOption match {
      case Some(entry) ⇒ Spreadsheet.spreadsheetInfo(entry) match {
        case Some(info) ⇒ go(entries.tail, infos :+ info)
        case _ ⇒ Future.failed[List[SpreadsheetInfo]](
          new IllegalArgumentException(s"Invalid spreadsheet entry: $entry"))
      }
      case _ ⇒ Future.successful(infos)
    }

    for {
      feed ← feed(_ ⇒ SpreadsheetClient.listRequest)
      infos ← Future(feed \\ "entry").flatMap(go(_, Nil))
    } yield infos
  }

  /**
   * Returns list of worksheets from given URL.
   * @see SpreadsheetInfo#worksheetsUrl
   */
  def worksheets(url: String): Future[List[WorksheetInfo]] = {
    @annotation.tailrec
    def go(entries: Seq[Node], infos: List[WorksheetInfo]): Future[List[WorksheetInfo]] = entries.headOption match {
      case Some(entry) ⇒ Spreadsheet.worksheetInfo(entry) match {
        case Some(info) ⇒ go(entries.tail, infos :+ info)
        case _ ⇒ Future.failed[List[WorksheetInfo]](
          new IllegalArgumentException(s"Invalid worksheet entry: $entry"))
      }
      case _ ⇒ Future.successful(infos)
    }

    for {
      feed ← feed(_.get(url))
      infos ← Future(feed \\ "entry").flatMap(go(_, Nil))
    } yield infos
  }

  /**
   * Returns list of cells from given URL.
   * @see WorksheetInfo#cellsUrl
   *
   * @param url Cells URL
   * @param rowRange Row range
   * @param colRange Column range
   */
  def cells(url: String, rowRange: Option[WorksheetRange] = None, colRange: Option[WorksheetRange] = None): Future[WorksheetCells] = for {
    feed ← feed { client ⇒
      val ps: List[NameValuePair] = (rowRange, colRange) match {
        case (Some(rr), Some(cr)) ⇒
          rangeParams("%s-row", rr) ++ rangeParams("%s-col", cr)
        case (_, Some(cr)) ⇒ rangeParams("%s-col", cr)
        case (Some(rr), _) ⇒ rangeParams("%s-row", rr)
        case _             ⇒ Nil
      }
      client.get(url, ps)
    }
    co ← Future(Spreadsheet.worksheetCells(feed))
    cells ← co.fold[Future[WorksheetCells]](
      Future.failed(new IllegalArgumentException(
        s"Invalid cells feed: $feed")))(cs ⇒ Future.successful(cs))
  } yield cells

  /**
   * Inserts a new row (must not already exist)
   * @param url Cells URL (`WorksheetInfo.cellsUrl`)
   * @param after Position of row after which new row must be inserted
   */
  def insertAfter(url: String, after: Int = 0): Future[Unit] = for {
    client ← Future(client acquireAndGet identity)
  } yield ???

  // ---

  import org.apache.http.client.methods.HttpRequestBase
  import org.apache.http.message.BasicNameValuePair

  @inline private def rangeParams(fmt: String, range: WorksheetRange): List[NameValuePair] = range match {
    case WorksheetRange(Some(min), Some(max)) ⇒ List(
      new BasicNameValuePair(fmt.format("min"), min.toString),
      new BasicNameValuePair(fmt.format("max"), max.toString))
    case WorksheetRange(_, Some(max)) ⇒
      List(new BasicNameValuePair(fmt.format("max"), max.toString))
    case WorksheetRange(Some(min), _) ⇒
      List(new BasicNameValuePair(fmt.format("min"), min.toString))
    case _ ⇒ Nil
  }

  private def feed(r: (HttpClient) ⇒ HttpRequestBase): Future[Elem] =
    Future {
      (for {
        client ← http.client
        val req = r(client)
        resp ← refreshToken.fold(
          client.execute(OAuthClient.prepare(req, accessToken)))(ref ⇒
            client.execute(OAuthClient.refreshable(req, accessToken,
              ref.clientId, ref.clientSecret, ref.token)))
        src ← if (resp.getStatusLine.getStatusCode == 200) {
          managed(new InputStreamReader(resp.getEntity.getContent)) map { r ⇒
            val src = new InputSource(r)
            src.setSystemId(req.getURI.toString)
            src
          }
        } else {
          sys.error(s"Fails to get feed: ${resp.getStatusLine}")
        }
      } yield src).acquireAndGet(XML.load)
    }
}

/** Companion for Spreadsheet DSL */
object Spreadsheet {
  /**
   * Instanciate access to Google Spreadsheet
   * using default HTTP implementation.
   *
   * @param accessTok Access token (can be refreshed if expired and refresh token provided)
   * @param refreshTok Optional refresh token
   *
   * {{{
   * import fr.applicius.foorgol.Spreadsheet
   *
   * Spreadsheet("mustNotBeExpired", None) // Cannot refresh access token
   *
   * Spreadsheer("offlineAccessToken", Some( // Can refresh if token is expired
   *   RefreshToken("clientId", "secret", "refreshToken")))
   * }}}
   */
  def apply(accessTok: String, refreshTok: Option[RefreshToken] = None)(implicit ctx: ExecutionContext): Spreadsheet = new Spreadsheet with WithHttp {
    val context = ctx
    val accessToken = accessTok
    val refreshToken = refreshTok
    def client = managed(HttpClient())
  }

  /** Extracts spreadsheet information from given XML entry. */
  private[foorgol] def spreadsheetInfo(entry: Node): Option[SpreadsheetInfo] =
    for {
      id ← (entry \ "id").headOption map { e ⇒
        val i = e.text
        val n = i.lastIndexOf('/')
        if (n == -1) i else i.drop(n + 1)
      }
      updated ← (entry \ "updated").headOption map { up ⇒
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).
          parse(up.text)
      }
      title ← (entry \ "title").headOption.map(_.text).
        orElse(Some(s"Spreadsheet $id"))
      (s, w) ← {
        (entry \ "link").foldLeft[(Option[String], Option[String])](
          None -> None) { (st, l) ⇒
            val (self, wrks) = st
            val rel = l.attribute("rel").headOption

            rel.map(_.text).fold(st) {
              case "self" ⇒ l.attribute("href").map(_.text) -> wrks
              case "http://schemas.google.com/spreadsheets/2006#worksheetsfeed" ⇒
                self -> l.attribute("href").map(_.text)

              case _ ⇒ st
            }
          } match {
            case (Some(s), Some(w)) ⇒ Some(s -> w)
            case _                  ⇒ None
          }
      }
    } yield SpreadsheetInfo(id, updated, title, s, w)

  /** Extracts worksheet information from given XML entry. */
  private[foorgol] def worksheetInfo(entry: Node): Option[WorksheetInfo] =
    for {
      id ← (entry \ "id").headOption map { e ⇒
        val i = e.text
        val n = i.lastIndexOf('/')
        if (n == -1) i else i.drop(n + 1)
      }
      updated ← (entry \ "updated").headOption map { up ⇒
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).
          parse(up.text)
      }
      title ← (entry \ "title").headOption.map(_.text).
        orElse(Some(s"Worksheet $id"))
      (s, c) ← {
        (entry \ "link").foldLeft[(Option[String], Option[String])](
          None -> None) { (st, l) ⇒
            val (self, cells) = st
            val rel = l.attribute("rel").headOption

            rel.map(_.text).fold(st) {
              case "self" ⇒ l.attribute("href").map(_.text) -> cells
              case "http://schemas.google.com/spreadsheets/2006#cellsfeed" ⇒
                self -> l.attribute("href").map(_.text)

              case _ ⇒ st
            }
          } match {
            case (Some(s), Some(c)) ⇒ Some(s -> c)
            case _                  ⇒ None
          }
      }
    } yield WorksheetInfo(id, updated, title, s, c)

  /** Extracts cells from worksheet feed. */
  private[foorgol] def worksheetCells(feed: Elem): Option[WorksheetCells] = {
    @annotation.tailrec
    def go(entries: Seq[Node], cs: List[WorksheetCell]): Option[List[WorksheetCell]] = entries.headOption match {
      case Some(entry) ⇒ worksheetCell(entry) match {
        case Some(c) ⇒ go(entries.tail, cs :+ c)
        case _       ⇒ None
      }
      case _ ⇒ Some(cs)
    }

    for {
      batch ← (feed \ "link").foldLeft[Option[String]](None) { (o, l) ⇒
        if (l.attribute("rel").exists(
          _.text == "http://schemas.google.com/g/2005#batch")) {
          l.attribute("href").headOption.map(_.text)
        } else o
      }
      count ← (feed \ "totalResults").headOption.map(_.text.toInt)
      cells ← go((feed \ "entry"), Nil)
    } yield WorksheetCells(count, batch, cells)
  }

  /** Extracts worksheet cell from given XML entry. */
  private[foorgol] def worksheetCell(entry: Node): Option[WorksheetCell] =
    for {
      id ← (entry \ "id").headOption map { e ⇒
        val i = e.text
        val n = i.lastIndexOf('/')
        if (n == -1) i else i.drop(n + 1)
      }
      title ← (entry \ "title").headOption.map(_.text).
        orElse(Some(s"Cell $id"))
      (s, e) ← {
        (entry \ "link").foldLeft[(Option[String], Option[String])](
          None -> None) { (st, l) ⇒
            val (self, edit) = st
            val rel = l.attribute("rel").headOption

            rel.map(_.text).fold(st) {
              case "self" ⇒ l.attribute("href").map(_.text) -> edit
              case "edit" ⇒ self -> l.attribute("href").map(_.text)
              case _      ⇒ st
            }
          } match {
            case (Some(s), Some(e)) ⇒ Some(s -> e)
            case _                  ⇒ None
          }
      }
      gsc ← (entry \ "cell").headOption
      iv ← gsc.attribute("inputValue").headOption.map(_.text)
      r ← gsc.attribute("row").headOption.map(_.text.toInt)
      c ← gsc.attribute("col").headOption.map(_.text.toInt)
    } yield WorksheetCell(id, title, s, e, r, c, iv)
}

case class SpreadsheetInfo(id: String, updated: Date, title: String,
  selfUrl: String, worksheetsUrl: String)

case class WorksheetInfo(id: String, updated: Date, title: String,
  selfUrl: String, cellsUrl: String)

case class WorksheetCells(
  totalCount: Int, batchUrl: String, cells: List[WorksheetCell])

case class WorksheetCell(id: String, title: String,
  selfUrl: String, editUrl: String,
  row: Int, col: Int, value: String)

sealed trait WorksheetRange {
  def min: Option[Int]
  def max: Option[Int]
}

object WorksheetRange {
  def full(start: Int, end: Int) = new WorksheetRange {
    val min = Some(start)
    val max = Some(end)
  }

  def openStarted(end: Int) = new WorksheetRange {
    val min = None
    val max = Some(end)
  }

  def openEnded(start: Int) = new WorksheetRange {
    val min = Some(start)
    val max = None
  }

  def unapply(range: WorksheetRange): Option[(Option[Int], Option[Int])] =
    Some(range.min -> range.max)
}
