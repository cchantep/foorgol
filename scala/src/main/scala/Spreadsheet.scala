package fr.applicius.foorgol

import java.io.InputStreamReader
import java.util.{ Date, Locale }
import java.text.SimpleDateFormat
import java.net.URI

import org.apache.http.{ HttpResponse, NameValuePair }
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.message.BasicNameValuePair

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

  /** Returns list of all available spreadsheets. */
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
   * Returns information for matching spreadsheet, if any.
   *
   * @param id Spreadsheet ID ([[SpreadsheetInfo.id]])
   */
  def spreadsheet(id: String): Future[SpreadsheetInfo] = for {
    feed ← feed(_.get(new URI(
      s"https://spreadsheets.google.com/feeds/spreadsheets/$id")))
    info ← (feed \\ "entry").headOption.flatMap(Spreadsheet.spreadsheetInfo).
      fold(Future.failed[SpreadsheetInfo](
        new RuntimeException(s"Invalid spreadsheet entry")))(
        Future.successful(_))
  } yield info

  /**
   * Returns list of worksheets for specified spreadsheet
   *
   * @param id Spreadsheet ID ([[SpreadsheetInfo.id]])
   */
  def worksheets(id: String): Future[List[WorksheetInfo]] =
    withWorksheets(id)(List.empty[WorksheetInfo]) { (l, w) ⇒ Right(l :+ w) }

  /**
   * Returns list of worksheets from given URI
   * ([[SpreadsheetInfo.worksheetsUri]]).
   */
  def worksheets(uri: URI): Future[List[WorksheetInfo]] = {
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
      feed ← feed(_.get(uri))
      infos ← Future(feed \\ "entry").flatMap(go(_, Nil))
    } yield infos
  }

  /**
   * Processes worksheets found for specified spreadsheet
   * ([[SpreadsheetInfo.worksheetsUri]]).
   *
   * @param id Spreadsheet ID
   * @param z Initial value for function `f`
   * @param f Function given current state and extracted information. Must return either a final value at `Left` if must not look at other worksheets, or updated value at `Right` to go on processing worksheets.
   *
   * {{{
   * // api: Spreadsheet
   *
   * // Will find the second worksheet...
   * val second: Future[Option[WorksheetInfo]] =
   *   api.worksheet("spreadsheetId")(1 -> None) { (st, w) =>
   *   val (i, _) = st
   *   if (i == 2) /* found some, so no next step */ Left(2 -> Some(w))
   *   else Right(i+1 -> None)
   * } map (_._2)
   * }}}
   */
  def withWorksheets[T](id: String)(z: T)(f: (T, WorksheetInfo) ⇒ Either[T, T]): Future[T] = {
    @annotation.tailrec
    def go(state: T, entries: Seq[Node]): Future[T] = entries.headOption match {
      case Some(entry) ⇒ Spreadsheet.worksheetInfo(entry) match {
        case w @ Some(info) ⇒ f(state, info) match {
          case Right(next) ⇒ go(next, entries.tail) // required to go next
          case Left(finval) ⇒ // final value, no need to look after
            Future.successful(finval)
        }
        case _ ⇒ Future.failed[T](
          new RuntimeException(s"Invalid worksheet entry: $entry"))
      }
      case _ ⇒ Future.successful[T](state)
    }

    for {
      xml ← feed(_.get(new URI(
        s"https://spreadsheets.google.com/feeds/worksheets/$id/private/full")))
      info ← Future(xml \\ "entry").flatMap(go(z, _))
    } yield info
  }

  /**
   * Returns worksheet from specified worksheet
   * whose index in feed is given one, if any.
   *
   * @param id Spreadsheet ID ([[SpreadsheetInfo.id]])
   * @param index Worksheet index in underlying feed (first = 0)
   * @see [[withWorksheets]]
   *
   * {{{
   * val first: Future[Option[WorksheetInfo]] =
   *   worksheet("spreadsheetId", 0)
   * }}}
   */
  def worksheet(id: String, index: Int): Future[Option[WorksheetInfo]] = {
    val I = index
    withWorksheets[(Int, Option[WorksheetInfo])](id)(0 -> None) {
      case ((I, _), matching) ⇒ Left(0 -> Some(matching))
      case ((i, v), _)        ⇒ Right(i + 1 -> None)
    } map (_._2)
  }

  /**
   * Returns worksheet matching given ID.
   *
   * @param spreadsheetId Spreadsheet ID ([[SpreadsheetInfo.id]])
   * @param worksheetId Worksheet ID ([[WorksheetInfo.id]])
   * @see [[withWorksheets]]
   */
  def worksheet(spreadsheetId: String, worksheetId: String): Future[Option[WorksheetInfo]] = {
    val ID = worksheetId
    withWorksheets(spreadsheetId)(None: Option[WorksheetInfo]) {
      case (_, w @ WorksheetInfo(ID, _, _, _, _)) ⇒ Left(Some(w))
      case (st, _)                                ⇒ Right(st)
    }
  }

  /**
   * Returns list of cells from given URI ([[WorksheetInfo.cellsUri]]).
   *
   * @param uri Cells URI
   * @param rowRange Row range
   * @param colRange Column range
   */
  def cells(uri: URI, rowRange: Option[WorksheetRange], colRange: Option[WorksheetRange]): Future[WorksheetCells] = for {
    feed ← feed { client ⇒
      val ps: List[NameValuePair] = (rowRange, colRange) match {
        case (Some(rr), Some(cr)) ⇒
          rangeParams("%s-row", rr) ++ rangeParams("%s-col", cr)
        case (_, Some(cr)) ⇒ rangeParams("%s-col", cr)
        case (Some(rr), _) ⇒ rangeParams("%s-row", rr)
        case _             ⇒ Nil
      }
      client.get(uri, ps)
    }
    co ← Future(Spreadsheet.worksheetCells(feed))
    cells ← co.fold[Future[WorksheetCells]](
      Future.failed(new IllegalArgumentException(
        s"Invalid cells feed: $feed")))(cs ⇒ Future.successful(cs))
  } yield cells

  /**
   * Returns matching cells, if any.
   *
   * @param id Spreadsheet ID
   * @param index Worksheet index (first = 0)
   * @param rowRange Optional row range
   * @param colRange Optional column range
   *
   * {{{
   * cells("spreadsheetId", 0) // all cells of first worksheet
   * }}}
   */
  def cells(id: String, index: Int, rowRange: Option[WorksheetRange], colRange: Option[WorksheetRange]): Future[WorksheetCells] = for {
    v ← worksheet(id, index)
    w ← v.fold(Future.failed[WorksheetInfo](new IllegalArgumentException(
      s"No matching worksheet: $id, $index")))(Future.successful)
    cs ← cells(w.cellsUri, rowRange, colRange)
  } yield cs

  /**
   * Changes cells. Any existing content will be overwrited .
   *
   * @param uri Cells URI ([[WorksheetInfo.cellsUri]])
   * @param cells List of cells to be created
   * @return List of version URI for each created cell
   */
  def change(uri: URI, cells: List[CellValue]): Future[List[URI]] = {
    @annotation.tailrec
    def versionUri(links: Seq[Node]): Option[URI] = links.headOption match {
      case Some(l) ⇒
        if (l.attribute("rel").exists(_.text == "edit")) {
          l.attribute("href").headOption match {
            case Some(veru) ⇒ Some(new URI(veru.text))
            case _          ⇒ versionUri(links.tail)
          }
        } else versionUri(links.tail)
      case _ ⇒ None
    }

    Future(http.client acquireAndGet { c ⇒
      val exec: HttpRequestBase ⇒ HttpClient ⇒ ManagedResource[HttpResponse] =
        refreshToken.fold({ req: HttpRequestBase ⇒
          { c: HttpClient ⇒ c.execute(OAuthClient.prepare(req, accessToken)) }
        }) { ref ⇒
          { req: HttpRequestBase ⇒
            { c: HttpClient ⇒
              c.execute(OAuthClient.refreshable(req, accessToken,
                ref.clientId, ref.clientSecret, ref.token))
            }
          }
        }

      cells.foldLeft(List.empty[URI]) { (l, cell) ⇒
        val req = c.post(uri, cellXml(uri, cell), "application/atom+xml")
        exec(req)(c) flatMap { r ⇒
          if (r.getStatusLine.getStatusCode != 201 /* Created */ ) {
            sys.error(s"Fails to create cell: $cell, cause: ${r.getStatusLine}")
            // Will be pushed into wrapping ManagedResource
          } else managed(new InputStreamReader(r.getEntity.getContent))
        } map { r ⇒
          val src = new InputSource(r)
          src.setSystemId(uri.toString)
          XML.load(src)
        } acquireAndGet { xml ⇒
          versionUri(xml \\ "link") match {
            case Some(vu) ⇒ l :+ vu
            case _ ⇒
              sys.error(s"Fails to extract version URI: $xml")
            // Will be pushed into wrapping ManagedResource
          }
        }
      }
    })
  }

  /**
   * Changes cell in specified worksheet.
   *
   * @param id Spreadsheet ID
   * @param index Worksheet index (first = 0)
   * @param cells List of cells to be created
   * @return List of version URIs for each created cell
   *
   * {{{
   * // Put "A" as content of cell at first row second column,
   * // first worksheet of specified spreadsheet
   * change("spreadsheetId", 0, List(CellValue(1, 2, "A")))
   * }}}
   */
  def change(id: String, index: Int, cells: List[CellValue]): Future[List[URI]] = for {
    v ← worksheet(id, index)
    w ← v.fold(Future.failed[WorksheetInfo](new IllegalArgumentException(
      s"No matching worksheet: $id, $index")))(Future.successful)
    vs ← change(w.cellsUri, cells)
  } yield vs

  /**
   * Changes cell in specified worksheet.
   *
   * @param spreadsheetId Spreadsheet ID ([[SpreadsheetInfo.id]])
   * @param worksheetId Worksheet ID ([[WorksheetInfo.id]])
   * @param cells List of cells to be created
   * @return List of version URIs for each created cell
   *
   * {{{
   * // Put "A" as content of cell at first row second column,
   * // first worksheet of specified spreadsheet
   * change("spreadsheetId", "worksheetId", List(CellValue(1, 2, "A")))
   * }}}
   */
  def change(spreadsheetId: String, worksheetId: String, cells: List[CellValue]): Future[List[URI]] = for {
    v ← worksheet(spreadsheetId, worksheetId)
    w ← v.fold(Future.failed[WorksheetInfo](new IllegalArgumentException(
      s"No matching worksheet: $spreadsheetId, $worksheetId")))(
      Future.successful)
    vs ← change(w.cellsUri, cells)
  } yield vs

  /*
   * Inserts given values as uninterrupted sequence of cells in a new row
   * at end of the specified worksheet.
   *
   * @param spreadsheetId Spreadsheet ID ([[SpreadsheetInfo.id]])
   * @param worksheetId Worksheet ID ([[SpreadsheetInfo.id]])
   * @param values Uninterrupted sequence of cell values
   * @param List of version URIs for each created cell
   *
   * {{{
   * // Put ("A", "B") in first and second columns or a new row
   * // at end of specified worksheet
   * append(spreadsheetId, worksheetId, List("A", "B"))
   * }}}
  def append(spreadsheetId: String, worksheetId: String, cells: List[String]): Future[List[URI]] = for {
    v ← worksheet(spreadsheetId, worksheetId)
    w ← v.fold(Future.failed[WorksheetInfo](new IllegalArgumentException(
      s"No matching worksheet: $spreadsheetId, $worksheetId")))(
      Future.successful)
    wc ← this.cells(w.cellsUri,
      Some(WorksheetRange.full(1, 1)), Some(WorksheetRange.full(1, 1)))
    row = wc.totalCount + 1
    values = cells.foldLeft(1 -> List.empty[CellValue]) { (st, c) ⇒
      val (p, l) = st
      p + 1 -> (l :+ CellValue(row, p, c))
    }._2
    vs ← change(w.cellsUri, values)
  } yield vs
   */

  // ---

  /**
   * @param uri Cells URI
   * @param cell Cell value
   */
  @inline private def cellXml(uri: URI, cell: CellValue): String = {
    val cellUrl = s"$uri/R${cell.row}C${cell.col}"
    s"""<entry xmlns="http://www.w3.org/2005/Atom" xmlns:gs="http://schemas.google.com/spreadsheets/2006"><id>$cellUrl</id><link rel="edit" type="application/atom+xml" href="$cellUrl"/><gs:cell row="${cell.row}" col="${cell.col}" inputValue="${cell.value}"/></entry>"""
  }

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
      info ← try {
        Some(SpreadsheetInfo(id, updated, title, new URI(s), new URI(w)))
      } catch {
        // Take care of malformed URI
        case _: Throwable ⇒ None
      }
    } yield info

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
      info ← try {
        Some(WorksheetInfo(id, updated, title, new URI(s), new URI(c)))
      } catch {
        // Take care of malformed URI
        case _: Throwable ⇒ None
      }
    } yield info

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
      info ← try {
        Some(WorksheetCells(count, new URI(batch), cells))
      } catch {
        case _: Throwable ⇒ None // Take care of malformed batch URI
      }
    } yield info
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
      info ← try {
        Some(WorksheetCell(id, title, new URI(s), new URI(e), r, c, iv))
      } catch {
        case _: Throwable ⇒ None // Take care of malformed URIs
      }
    } yield info
}

case class SpreadsheetInfo(id: String, updated: Date, title: String,
  selfUri: URI, worksheetsUri: URI)

/**
 * Information about a single worksheet.
 */
case class WorksheetInfo(
  /** Unique ID */
  id: String,

  /** Time it was last updated */
  updated: Date,

  /** Worksheet title */
  title: String,

  /** URI to locate this information */
  selfUri: URI,

  /** URI to locate worksheet content (cells feed) */
  cellsUri: URI)

/** Worksheet content (cells) */
case class WorksheetCells(
  /** Count of all rows in the worksheet */
  totalCount: Int,

  /** URI for batch update */
  batchUri: URI,

  /** Matching cells. */
  cells: List[WorksheetCell])

case class WorksheetCell(id: String, title: String,
  selfUri: URI, editUri: URI,
  row: Int, col: Int, value: String)

/** Cell content */
case class CellValue(
  /** Row position (first = 1) */
  row: Int,

  /** Column position (first = 1) */
  col: Int,

  /** Cell value */
  value: String)

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
