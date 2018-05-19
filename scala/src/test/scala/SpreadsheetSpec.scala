package foorgol

import java.net.URI

import org.specs2.concurrent.ExecutionEnv
import scala.concurrent.duration.Duration

class SpreadsheetSpec(implicit ee: ExecutionEnv)
    extends org.specs2.mutable.Specification with SpreadsheetFixtures {

  "Spreadsheet" title

  "Spreadsheet list" should {
    "be successfully fetched" in {
      val mock = MockSpreadsheet("accessToken1", None) {
        MockHttpClient(_ ⇒ resp1)
      }

      mock.list aka "list" must beEqualTo(infos1).await
    }
  }

  "Single spreadsheet" should {
    "be successfully found by ID" in {
      var reqUri: String = null
      val mock = MockSpreadsheet("accessToken7", None) {
        MockHttpClient { req ⇒ reqUri = req.getURI.toString; resp7 }
      }

      mock.spreadsheet("_id1") aka "response" must beLike[SpreadsheetInfo] {
        case info ⇒
          reqUri aka "request URI" mustEqual (
            "https://spreadsheets.google.com/feeds/spreadsheets/_id1") and (
              info aka "spreadsheet" must_== infos3)
      }.await
    }
  }

  "Worksheets" should {
    "be successfully listed by URI" in {
      var reqUri: String = null
      val mock = MockSpreadsheet("accessToken2", None) {
        MockHttpClient { req ⇒ reqUri = req.getURI.toString; resp2 }
      }

      mock.worksheets(new URI("http://worksheets-uri")).
        aka("response") must beLike[List[WorksheetInfo]] {
          case res ⇒
            reqUri aka "request URI" must_== "http://worksheets-uri" and (
              res aka "worksheets" must_== infos2)
        }.await
    }

    "be successfully processed to get self URI of first one" in {
      var reqUri: String = null
      val mock = MockSpreadsheet("accessToken8", None) {
        MockHttpClient { req ⇒ reqUri = req.getURI.toString; resp2 }
      }

      mock.withWorksheets[(Int, Option[URI])]("_id2")(0 -> None) { (st, w) ⇒
        val (i, _) = st
        if (i == 0) Left(0 -> Some(w.selfUri)) // matches on first
        else {
          val x: Int = i
          val e: Either[(Int, Option[URI]), (Int, Option[URI])] = Right(i + 1 -> None)
          e
        }
      } aka "processed" must beLike[(Int, Option[URI])] {
        case (0, selfUri) ⇒
          reqUri aka "request URI" must_== ("https://spreadsheets.google.com/feeds/worksheets/_id2/private/full") and (
            selfUri aka "URI of first worksheet" must beSome.which {
              _ aka "self URI" must_== new URI("https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full/od6")
            })
      }.await
    }

    "be successfully listed by spreadsheet ID" in {
      var reqUri: String = null
      val mock = MockSpreadsheet("accessToken13", None) {
        MockHttpClient { req ⇒ reqUri = req.getURI.toString; resp2 }
      }

      mock.worksheets("_spreadsheetId1").
        aka("response") must beLike[List[WorksheetInfo]] {
          case res ⇒
            reqUri aka "request URI" must_== ("https://spreadsheets.google.com/feeds/worksheets/_spreadsheetId1/private/full") and (
              res aka "worksheets" must_== infos2)
        }.await
    }
  }

  "Single worksheet" should {
    "be found by index 0" in {
      var reqUri: String = null
      val mock = MockSpreadsheet("accessToken9", None) {
        MockHttpClient { req ⇒ reqUri = req.getURI.toString; resp2 }
      }

      mock.worksheet("_id3", 0) aka "response" must beSome[WorksheetInfo].
        which {
          _ aka "first worksheet" must_== infos2(0) and (
            reqUri aka "request URI" must_== "https://spreadsheets.google.com/feeds/worksheets/_id3/private/full")
        }.await
    }

    "not be found by index 1" in {
      val mock = MockSpreadsheet("accessToken9", None) {
        MockHttpClient(_ ⇒ resp2)
      }

      mock.worksheet("_id3", 1) aka "response" must beNone.await
    }

    "be found by ID (od6)" in {
      var reqUri: String = null
      val mock = MockSpreadsheet("accessToken14", None) {
        MockHttpClient { req ⇒ reqUri = req.getURI.toString; resp2 }
      }

      mock.worksheet("_spreadsheetId2", "od6").
        aka("response") must beSome[WorksheetInfo].which {
          _ aka "matching worksheet" must_== infos2(0) and (
            reqUri aka "request URI" must_== "https://spreadsheets.google.com/feeds/worksheets/_spreadsheetId2/private/full")
        }.await
    }

    "not be found by ID (not_found)" in {
      var reqUri: String = null
      val mock = MockSpreadsheet("accessToken15", None) {
        MockHttpClient { req ⇒ reqUri = req.getURI.toString; resp2 }
      }

      mock.worksheet("_spreadsheetId2", "not_found").
        aka("response") must beNone.await
    }
  }

  "Cells list" should {
    "be successfully fetched without range" in {
      var reqUri: String = null
      val mock = MockSpreadsheet("accessToken3", None) {
        MockHttpClient { req ⇒ reqUri = req.getURI.toString; resp3 }
      }

      mock.cells(new URI("http://cells-uri"), None, None).
        aka("response") must beSome[WorksheetCells].which { res ⇒
          reqUri aka "request URI" must_== "http://cells-uri" and (
            res aka "cells" must_== cells1)
        }.await
    }

    "be successfully fetched with a full row range" in {
      var reqUri: String = null
      val mock = MockSpreadsheet("accessToken4", None) {
        MockHttpClient { req ⇒ reqUri = req.getURI.toString; resp4 }
      }

      mock.cells(new URI("http://cells-uri"),
        Some(WorksheetRange.full(1, 2)), None).
        aka("response") must beSome[WorksheetCells].which { res ⇒
          reqUri aka "request URI" mustEqual (
            "http://cells-uri?min-row=1&max-row=2") and (
              res aka "cells" must_== cells2)
        }.await
    }

    "be successfully fetched with an open started column range" in {
      var reqUri: String = null
      val mock = MockSpreadsheet("accessToken5", None) {
        MockHttpClient { req ⇒ reqUri = req.getURI.toString; resp5 }
      }

      mock.cells(new URI("http://cells-uri"),
        None, Some(WorksheetRange.openStarted(1))).
        aka("response") must beSome[WorksheetCells].which { res ⇒
          reqUri aka "request URI" mustEqual (
            "http://cells-uri?max-col=1") and (res aka "cells" must_== cells3)
        }.await
    }

    "be successfully found by spreadsheet ID and worksheet index, with an open ended row range" in {
      val resp = resp9
      val reqs = scala.collection.mutable.MutableList.empty[String]
      val mock = MockSpreadsheet("accessToken12", None) {
        MockHttpClient { req ⇒ reqs += req.getURI.toString; resp.next }
      }

      mock.cells("_id5", 0, Some(WorksheetRange.openEnded(2)), None).
        aka("response") must beSome[WorksheetCells].which { res ⇒
          reqs.toList aka "request URIs" must_== List(
            "https://spreadsheets.google.com/feeds/worksheets/_id5/private/full", "https://spreadsheets.google.com/feeds/cells/178A6E9A-34EB-435E-B483-61E2169AEB79/od6/private/full?min-row=2") and (res aka "cells" must_== cells2)
        }.await
    }

    "be successfully found by spreadsheet ID and worksheet ID with a full column range" in {
      var reqUri: String = null
      val mock = MockSpreadsheet("accessToken16", None) {
        MockHttpClient { req ⇒ reqUri = req.getURI.toString; resp5 }
      }

      mock.cells("_spreadsheetId4", "_worksheetId1", None, Some(WorksheetRange.full(1, 2))).
        aka("response") must beSome[WorksheetCells].which { res ⇒
          reqUri aka "request URI" must_== ("https://spreadsheets.google.com/feeds/cells/_spreadsheetId4/_worksheetId1/private/full?min-col=1&max-col=2") and (res aka "cells" must_== cells3)
        }.await
    }
  }

  "Last row" should {
    "be found for specified worksheet with existing cells" in {
      val reqs = scala.collection.mutable.MutableList.empty[String]
      val mock = MockSpreadsheet("accessToken17", None) {
        MockHttpClient { req ⇒ reqs += req.getURI.toString; resp5 }
      }

      mock.lastRow("_spreadsheetId5", "_worksheetId2").
        aka("response") must beSome[WorksheetCells].which { res ⇒
          reqs.toList aka "request URIs" must_== List("https://spreadsheets.google.com/feeds/cells/_spreadsheetId5/_worksheetId2/private/basic?max-results=1", "https://spreadsheets.google.com/feeds/cells/_spreadsheetId5/_worksheetId2/private/full?start-index=5") and (res aka "cells" must_== cells3)
        }.await
    }

    "not be found for specified worksheet with no cell" in {
      val resp = resp11
      val reqs = scala.collection.mutable.MutableList.empty[String]
      val mock = MockSpreadsheet("accessToken18", None) {
        MockHttpClient { req ⇒ reqs += req.getURI.toString; resp.next }
      }

      mock.lastRow("_spreadsheetId6", "_worksheetId2").
        aka("response") must beLike[Option[WorksheetCells]] {
          case res ⇒
            reqs.toList aka "request URIs" must_== List("https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId2/private/basic?max-results=1") and (res aka "last row" must beNone)
        }.await
    }

    "fail to be checked when worksheet is not found" in {
      val resp = resp12
      val reqs = scala.collection.mutable.MutableList.empty[String]
      val mock = MockSpreadsheet("accessToken18", None) {
        MockHttpClient { req ⇒ reqs += req.getURI.toString; resp.next }
      }

      scala.concurrent.Await.result(
        mock.lastRow("_spreadsheetId7", "_worksheetId2"),
        Duration(2, "s")) aka "result" must throwA[Exception].like {
          case ex ⇒
            ex.getMessage aka "error" must_== (
              "Cannot find worksheet: _worksheetId2 (_spreadsheetId7)") and (
                reqs.toList aka "request URIs" must_== List("https://spreadsheets.google.com/feeds/cells/_spreadsheetId7/_worksheetId2/private/basic?max-results=1"))
        }
    }
  }

  "Cell changes" should {
    "be successfully applied by URI for cells (4, 1) and (4, 3)" in {
      val resp = resp6
      val bodys = scala.collection.mutable.MutableList.empty[String]
      val mock = MockSpreadsheet("accessToken6", None) {
        MockHttpClient {
          case req: org.apache.http.client.methods.HttpPost ⇒
            val buf = new java.io.ByteArrayOutputStream
            req.getEntity.writeTo(buf)
            bodys += new String(buf.toByteArray)

            resp.next
        }
      }

      mock.change(new URI("http://cells-uri"), List(
        CellValue(4, 1, "4_1"), CellValue(4, 3, "4_3"))).
        aka("response") must beLike[List[URI]] {
          case res ⇒ bodys.toList aka "requests" must_== body6 and (
            res aka "version URIs" must_== changes1)
        }.await
    }

    "be successfully applied for cells (4, 1) and (4, 3) in first worksheet of specified spreadsheet" in {
      val resp = resp8
      val bodys = scala.collection.mutable.MutableList.empty[String]
      val mock = MockSpreadsheet("accessToken10", None) {
        MockHttpClient {
          case req: org.apache.http.client.methods.HttpPost ⇒
            val buf = new java.io.ByteArrayOutputStream
            req.getEntity.writeTo(buf)
            bodys += new String(buf.toByteArray)

            resp.next
          case r ⇒ resp.next
        }
      }

      mock.change("_id4", 0, List(
        CellValue(4, 1, "4_1"), CellValue(4, 3, "4_3"))).
        aka("response") must beLike[List[URI]] {
          case res ⇒
            bodys.toList aka "requests" must_== body7 and (
              res aka "version URI" must_== changes1)
        }.await
    }

    "fail for cells in second worksheet of specified spreadsheet" in {
      val mock = MockSpreadsheet("accessToken11", None) {
        MockHttpClient { _ ⇒ resp2 }
      }

      scala.concurrent.Await.result(
        mock.change("_id4", 1, List(CellValue(4, 3, "4_3"))),
        Duration(2, "s")).
        aka("result") must throwA[IllegalArgumentException](
          "No matching worksheet: _id4, 1")

    }

    "be successfully applied for cells (4, 1) and (4, 3) in specified worksheet (od6)" in {
      val resp = resp6
      val bodys = scala.collection.mutable.MutableList.empty[String]
      val mock = MockSpreadsheet("accessToken15", None) {
        MockHttpClient {
          case req: org.apache.http.client.methods.HttpPost ⇒
            val buf = new java.io.ByteArrayOutputStream
            req.getEntity.writeTo(buf)
            bodys += new String(buf.toByteArray)

            resp.next
          case r ⇒ resp.next
        }
      }

      mock.change("_spreadsheetId3", "od6", List(
        CellValue(4, 1, "4_1"), CellValue(4, 3, "4_3"))).
        aka("response") must beLike[List[URI]] {
          case res ⇒
            bodys.toList aka "requests" must_== body9 and (
              res aka "version URI" must_== changes1)
        }.await
    }

    "fail for cells with invalid worksheet ID (not_found)" in {
      val mock = MockSpreadsheet("accessToken16", None) {
        MockHttpClient(_ ⇒ badRequest)
      }

      scala.concurrent.Await.result(
        mock.change("_spreadsheetId3", "not_found",
          List(CellValue(4, 3, "4_3"))), Duration(2, "s")).
        aka("result") must throwA[RuntimeException].like {
          case e ⇒ e.getMessage aka "error" must startWith(
            "Fails to create cell: CellValue(4,3,4_3)")
        }
    }

    "append a new row at end of specified worksheet" in {
      val resp = resp10
      val reqs = scala.collection.mutable.MutableList.empty[String]
      var body: String = null
      val mock = MockSpreadsheet("accessToken18", None) {
        MockHttpClient {
          case post: org.apache.http.client.methods.HttpPost ⇒
            val buf = new java.io.ByteArrayOutputStream
            post.getEntity.writeTo(buf)

            body = new String(buf.toByteArray)
            reqs += post.getURI.toString

            resp.next
          case req ⇒ reqs += req.getURI.toString; resp.next
        }
      }

      mock.append("_spreadsheetId6", "_worksheetId3", "appended").
        aka("response") must beLike[List[URI]] {
          case res ⇒
            reqs.toList aka "request URIs" must_== List(
              "https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/basic?max-results=1", "https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full?start-index=5", "https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full") and (
                body aka "append request body" must_== body10) and (
                  res aka "version URI" must_== List(new URI("https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full/R4C1/7clm")))
        }.await
    }
  }
}

sealed trait MockSpreadsheet extends Spreadsheet { http: WithHttp ⇒
  def context = scala.concurrent.ExecutionContext.Implicits.global
}

object MockSpreadsheet {
  def apply(accessTok: String, refreshTok: Option[RefreshToken] = None)(cli: MockHttpClient = MockHttpClient()) = new MockSpreadsheet with WithHttp {
    val accessToken = accessTok
    val refreshToken = refreshTok
    val client = resource.managed(cli)
  }
}

sealed trait SpreadsheetFixtures {
  import java.text.SimpleDateFormat
  import java.util.Locale

  import org.apache.http.ProtocolVersion
  import org.apache.http.entity.StringEntity
  import org.apache.http.message.{BasicHttpResponse, BasicStatusLine}

  val httpProto = new ProtocolVersion("http", 1, 1)

  val resp1 = {
    val ent = new StringEntity("""<feed xmlns:openSearch="http://a9.com/-/spec/opensearchrss/1.0/" xmlns="http://www.w3.org/2005/Atom"><id>https://spreadsheets.google.com/feeds/spreadsheets/private/full</id><updated>2014-09-04T13:31:28.348Z</updated><category term="http://schemas.google.com/spreadsheets/2006#spreadsheet" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">Available Spreadsheets - email@google.com</title><link href="http://docs.google.com" type="text/html" rel="alternate"/><link href="https://spreadsheets.google.com/feeds/spreadsheets/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#feed"/><link href="https://spreadsheets.google.com/feeds/spreadsheets/private/full" type="application/atom+xml" rel="self"/><openSearch:totalResults>2</openSearch:totalResults><openSearch:startIndex>1</openSearch:startIndex><entry><id>https://spreadsheets.google.com/feeds/spreadsheets/private/full/D5498A33-8CB0-4AD9-84AD-996566CB7BFE</id><updated>2014-08-31T15:11:45.892Z</updated><category term="http://schemas.google.com/spreadsheets/2006#spreadsheet" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">Sheet 1</title><content type="text">Sheet 1</content><link href="https://spreadsheets.google.com/feeds/worksheets/D5498A33-8CB0-4AD9-84AD-996566CB7BFE/private/full" type="application/atom+xml" rel="http://schemas.google.com/spreadsheets/2006#worksheetsfeed"/><link href="https://docs.google.com/spreadsheets/d/D5498A33-8CB0-4AD9-84AD-996566CB7BFE/edit" type="text/html" rel="alternate"/><link href="https://spreadsheets.google.com/feeds/spreadsheets/private/full/D5498A33-8CB0-4AD9-84AD-996566CB7BFE" type="application/atom+xml" rel="self"/><author><name>google</name><email>email@google.com</email></author></entry><entry><id>https://spreadsheets.google.com/feeds/spreadsheets/private/full/17vQxA3Yqsuu9knmahflx6LW60JO2_VMaZPy62zx5Bz8</id><updated>2014-08-28T18:32:54.299Z</updated><category term="http://schemas.google.com/spreadsheets/2006#spreadsheet" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">2.Sheet</title><content type="text">2.Sheet</content><link href="https://spreadsheets.google.com/feeds/worksheets/17vQxA3Yqsuu9knmahflx6LW60JO2_VMaZPy62zx5Bz8/private/full" type="application/atom+xml" rel="http://schemas.google.com/spreadsheets/2006#worksheetsfeed"/><link href="https://docs.google.com/spreadsheets/d/17vQxA3Yqsuu9knmahflx6LW60JO2_VMaZPy62zx5Bz8/edit" type="text/html" rel="alternate"/><link href="https://spreadsheets.google.com/feeds/spreadsheets/private/full/17vQxA3Yqsuu9knmahflx6LW60JO2_VMaZPy62zx5Bz8" type="application/atom+xml" rel="self"/><author><name>google</name><email>email@google.com</email></author></entry></feed>""")
    val resp = new BasicHttpResponse(new BasicStatusLine(httpProto, 200, "OK"))
    resp.setEntity(ent)
    resp
  }

  val infos1 = {
    val datefmt =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    List(
      SpreadsheetInfo(
        id = "D5498A33-8CB0-4AD9-84AD-996566CB7BFE",
        updated = datefmt.parse("2014-08-31T15:11:45.892Z"),
        title = "Sheet 1",
        selfUri = new URI("https://spreadsheets.google.com/feeds/spreadsheets/private/full/D5498A33-8CB0-4AD9-84AD-996566CB7BFE"),
        worksheetsUri = new URI("https://spreadsheets.google.com/feeds/worksheets/D5498A33-8CB0-4AD9-84AD-996566CB7BFE/private/full")),
      SpreadsheetInfo(
        id = "17vQxA3Yqsuu9knmahflx6LW60JO2_VMaZPy62zx5Bz8",
        updated = datefmt.parse("2014-08-28T18:32:54.299Z"),
        title = "2.Sheet",
        selfUri = new URI("https://spreadsheets.google.com/feeds/spreadsheets/private/full/17vQxA3Yqsuu9knmahflx6LW60JO2_VMaZPy62zx5Bz8"),
        worksheetsUri = new URI("https://spreadsheets.google.com/feeds/worksheets/17vQxA3Yqsuu9knmahflx6LW60JO2_VMaZPy62zx5Bz8/private/full")))
  }

  val resp2 = {
    val ent = new StringEntity("""<feed xmlns:gs="http://schemas.google.com/spreadsheets/2006" xmlns:openSearch="http://a9.com/-/spec/opensearchrss/1.0/" xmlns="http://www.w3.org/2005/Atom"><id>https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#worksheet" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">2.Sheet</title><link href="https://docs.google.com/spreadsheets/d/178A6E9A-34EB-435E-B483-61E2169AEB79/edit" type="application/atom+xml" rel="alternate"/><link href="https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#feed"/><link href="https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#post"/><link href="https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full" type="application/atom+xml" rel="self"/><author><name>google</name><email>test@ema.il</email></author><openSearch:totalResults>1</openSearch:totalResults><openSearch:startIndex>1</openSearch:startIndex><entry><id>https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full/od6</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#worksheet" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">Worksheet 1</title><content type="text">Worksheet 1</content><link href="https://spreadsheets.google.com/feeds/list/178A6E9A-34EB-435E-B483-61E2169AEB79/od6/private/full" type="application/atom+xml" rel="http://schemas.google.com/spreadsheets/2006#listfeed"/><link href="https://spreadsheets.google.com/feeds/cells/178A6E9A-34EB-435E-B483-61E2169AEB79/od6/private/full" type="application/atom+xml" rel="http://schemas.google.com/spreadsheets/2006#cellsfeed"/><link href="https://docs.google.com/spreadsheets/d/178A6E9A-34EB-435E-B483-61E2169AEB79/gviz/tq?gid=0" type="application/atom+xml" rel="http://schemas.google.com/visualization/2008#visualizationApi"/><link href="https://docs.google.com/spreadsheets/d/178A6E9A-34EB-435E-B483-61E2169AEB79/export?gid=0&amp;format=csv" type="text/csv" rel="http://schemas.google.com/spreadsheets/2006#exportcsv"/><link href="https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full/od6" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full/od6/u9oqmj" type="application/atom+xml" rel="edit"/><gs:colCount>26</gs:colCount><gs:rowCount>1000</gs:rowCount></entry></feed>""")
    val resp = new BasicHttpResponse(new BasicStatusLine(httpProto, 200, "OK"))
    resp.setEntity(ent)
    resp
  }

  val infos2 = {
    val datefmt =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    List(WorksheetInfo(
      id = "od6",
      updated = datefmt.parse("2014-08-28T18:32:54.266Z"),
      title = "Worksheet 1",
      selfUri = new URI("https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full/od6"),
      cellsUri = new URI("https://spreadsheets.google.com/feeds/cells/178A6E9A-34EB-435E-B483-61E2169AEB79/od6/private/full")))
  }

  val resp3 = {
    val ent = new StringEntity("""<feed xmlns:batch="http://schemas.google.com/gdata/batch" xmlns:gs="http://schemas.google.com/spreadsheets/2006" xmlns:openSearch="http://a9.com/-/spec/opensearchrss/1.0/" xmlns="http://www.w3.org/2005/Atom"><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">1.Sheet</title><link href="https://docs.google.com/spreadsheets/d/8F53A111-4351-4E7E-A460-51D31F03D599/edit" type="application/atom+xml" rel="alternate"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#feed"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#post"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/batch" type="application/atom+xml" rel="http://schemas.google.com/g/2005#batch"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full" type="application/atom+xml" rel="self"/><author><name>google</name><email>test@ema.il</email></author><openSearch:totalResults>5</openSearch:totalResults><openSearch:startIndex>1</openSearch:startIndex><gs:rowCount>1000</gs:rowCount><gs:colCount>26</gs:colCount><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">A1</title><content type="text">X</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1/2g" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Val.1" col="1" row="1">Val.1</gs:cell></entry><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">B1</title><content type="text">Y</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2/9zlg9" type="application/atom+xml" rel="edit"/><gs:cell inputValue="2# Val" col="2" row="1">2# Val</gs:cell></entry><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">A2</title><content type="text">Yoyo</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1/1olos" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Test" col="1" row="2">Test</gs:cell></entry><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">B2</title><content type="text">Kaka</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2/bc5e4" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Value 4" col="2" row="2">Value 4</gs:cell></entry><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">A3</title><content type="text">Pouet</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1/19y351" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Test 5" col="1" row="3">Test 5</gs:cell></entry></feed>""")
    val resp = new BasicHttpResponse(new BasicStatusLine(httpProto, 200, "OK"))
    resp.setEntity(ent)
    resp
  }

  val cells1 = WorksheetCells(
    totalCount = 5,
    batchUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/batch"),
    cells = List(
      WorksheetCell(
        id = "R1C1", title = "A1",
        selfUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1"),
        editUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1/2g"),
        row = 1, col = 1,
        value = "Val.1"),
      WorksheetCell(
        id = "R1C2", title = "B1",
        selfUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2"),
        editUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2/9zlg9"),
        row = 1, col = 2,
        value = "2# Val"),
      WorksheetCell(
        id = "R2C1", title = "A2",
        selfUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1"),
        editUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1/1olos"),
        row = 2, col = 1,
        value = "Test"),
      WorksheetCell(
        id = "R2C2", title = "B2",
        selfUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2"),
        editUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2/bc5e4"),
        row = 2, col = 2,
        value = "Value 4"),
      WorksheetCell(
        id = "R3C1", title = "A3",
        selfUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1"),
        editUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1/19y351"),
        row = 3, col = 1,
        value = "Test 5")))

  val resp4 = {
    val ent = new StringEntity("""<feed xmlns:batch="http://schemas.google.com/gdata/batch" xmlns:gs="http://schemas.google.com/spreadsheets/2006" xmlns:openSearch="http://a9.com/-/spec/opensearchrss/1.0/" xmlns="http://www.w3.org/2005/Atom"><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">1.Sheet</title><link href="https://docs.google.com/spreadsheets/d/8F53A111-4351-4E7E-A460-51D31F03D599/edit" type="application/atom+xml" rel="alternate"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#feed"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#post"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/batch" type="application/atom+xml" rel="http://schemas.google.com/g/2005#batch"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full" type="application/atom+xml" rel="self"/><author><name>google</name><email>test@ema.il</email></author><openSearch:totalResults>5</openSearch:totalResults><openSearch:startIndex>1</openSearch:startIndex><gs:rowCount>1000</gs:rowCount><gs:colCount>26</gs:colCount><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">A1</title><content type="text">X</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1/2g" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Val.1" col="1" row="1">Val.1</gs:cell></entry><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">B1</title><content type="text">Y</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2/9zlg9" type="application/atom+xml" rel="edit"/><gs:cell inputValue="2# Val" col="2" row="1">2# Val</gs:cell></entry><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">A2</title><content type="text">Yoyo</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1/1olos" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Test" col="1" row="2">Test</gs:cell></entry><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">B2</title><content type="text">Kaka</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2/bc5e4" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Value 4" col="2" row="2">Value 4</gs:cell></entry></feed>""")
    val resp = new BasicHttpResponse(new BasicStatusLine(httpProto, 200, "OK"))
    resp.setEntity(ent)
    resp
  }

  val cells2 = WorksheetCells(
    totalCount = 5,
    batchUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/batch"),
    cells = List(
      WorksheetCell(
        id = "R1C1", title = "A1",
        selfUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1"),
        editUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1/2g"),
        row = 1, col = 1,
        value = "Val.1"),
      WorksheetCell(
        id = "R1C2", title = "B1",
        selfUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2"),
        editUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2/9zlg9"),
        row = 1, col = 2,
        value = "2# Val"),
      WorksheetCell(
        id = "R2C1", title = "A2",
        selfUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1"),
        editUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1/1olos"),
        row = 2, col = 1,
        value = "Test"),
      WorksheetCell(
        id = "R2C2", title = "B2",
        selfUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2"),
        editUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2/bc5e4"),
        row = 2, col = 2,
        value = "Value 4")))

  val resp5 = {
    val ent = new StringEntity("""<feed xmlns:batch="http://schemas.google.com/gdata/batch" xmlns:gs="http://schemas.google.com/spreadsheets/2006" xmlns:openSearch="http://a9.com/-/spec/opensearchrss/1.0/" xmlns="http://www.w3.org/2005/Atom"><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">1.Sheet</title><link href="https://docs.google.com/spreadsheets/d/8F53A111-4351-4E7E-A460-51D31F03D599/edit" type="application/atom+xml" rel="alternate"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#feed"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#post"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/batch" type="application/atom+xml" rel="http://schemas.google.com/g/2005#batch"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full" type="application/atom+xml" rel="self"/><author><name>google</name><email>test@ema.il</email></author><openSearch:totalResults>5</openSearch:totalResults><openSearch:startIndex>1</openSearch:startIndex><gs:rowCount>1000</gs:rowCount><gs:colCount>26</gs:colCount><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">A1</title><content type="text">X</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1/2g" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Val.1" col="1" row="1">Val.1</gs:cell></entry><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">A2</title><content type="text">Yoyo</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1/1olos" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Test" col="1" row="2">Test</gs:cell></entry><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">A3</title><content type="text">Pouet</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1/19y351" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Test 5" col="1" row="3">Test 5</gs:cell></entry></feed>""")
    val resp = new BasicHttpResponse(new BasicStatusLine(httpProto, 200, "OK"))
    resp.setEntity(ent)
    resp
  }

  val cells3 = WorksheetCells(
    totalCount = 5,
    batchUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/batch"),
    cells = List(
      WorksheetCell(
        id = "R1C1", title = "A1",
        selfUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1"),
        editUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1/2g"),
        row = 1, col = 1,
        value = "Val.1"),
      WorksheetCell(
        id = "R2C1", title = "A2",
        selfUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1"),
        editUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1/1olos"),
        row = 2, col = 1,
        value = "Test"),
      WorksheetCell(
        id = "R3C1", title = "A3",
        selfUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1"),
        editUri = new URI("https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1/19y351"),
        row = 3, col = 1,
        value = "Test 5")))

  def resp6 = {
    val ent1 = new StringEntity("""<?xml version='1.0' encoding='UTF-8'?><entry xmlns='http://www.w3.org/2005/Atom' xmlns:gs='http://schemas.google.com/spreadsheets/2006' xmlns:batch='http://schemas.google.com/gdata/batch'><id>https://spreadsheets.google.com/feeds/cells/872A3CBF-0EEF-41FD-8D21-23E79614393A/od6/private/full/R4C1</id><updated>2014-08-28T18:32:54.266Z</updated><category scheme='http://schemas.google.com/spreadsheets/2006' term='http://schemas.google.com/spreadsheets/2006#cell'/><title type='text'>A4</title><content type='text'>4_1</content><link rel='self' type='application/atom+xml' href='https://spreadsheets.google.com/feeds/cells/872A3CBF-0EEF-41FD-8D21-23E79614393A/od6/private/full/R4C1'/><link rel='edit' type='application/atom+xml' href='https://spreadsheets.google.com/feeds/cells/872A3CBF-0EEF-41FD-8D21-23E79614393A/od6/private/full/R4C1/5ckm'/><gs:cell row='4' col='1' inputValue='4_1'>4_1</gs:cell></entry>""")

    val respA = new BasicHttpResponse(
      new BasicStatusLine(httpProto, 201, "Created"))
    respA.setEntity(ent1)

    val ent2 = new StringEntity("""<?xml version='1.0' encoding='UTF-8'?><entry xmlns='http://www.w3.org/2005/Atom' xmlns:gs='http://schemas.google.com/spreadsheets/2006' xmlns:batch='http://schemas.google.com/gdata/batch'><id>https://spreadsheets.google.com/feeds/cells/872A3CBF-0EEF-41FD-8D21-23E79614393A/od6/private/full/R4C3</id><updated>2014-09-05T13:08:53.370Z</updated><category scheme='http://schemas.google.com/spreadsheets/2006' term='http://schemas.google.com/spreadsheets/2006#cell'/><title type='text'>C4</title><content type='text'>4_3</content><link rel='self' type='application/atom+xml' href='https://spreadsheets.google.com/feeds/cells/872A3CBF-0EEF-41FD-8D21-23E79614393A/od6/private/full/R4C3'/><link rel='edit' type='application/atom+xml' href='https://spreadsheets.google.com/feeds/cells/872A3CBF-0EEF-41FD-8D21-23E79614393A/od6/private/full/R4C3/k4jc8'/><gs:cell row='4' col='3' inputValue='4_3'>4_3</gs:cell></entry>""")

    val respB = new BasicHttpResponse(
      new BasicStatusLine(httpProto, 201, "Created"))
    respB.setEntity(ent2)

    List(respA, respB).iterator
  }

  val body6 = List(
    """<entry xmlns="http://www.w3.org/2005/Atom" xmlns:gs="http://schemas.google.com/spreadsheets/2006"><id>http://cells-uri/R4C1</id><link rel="edit" type="application/atom+xml" href="http://cells-uri/R4C1"/><gs:cell row="4" col="1" inputValue="4_1"/></entry>""",
    """<entry xmlns="http://www.w3.org/2005/Atom" xmlns:gs="http://schemas.google.com/spreadsheets/2006"><id>http://cells-uri/R4C3</id><link rel="edit" type="application/atom+xml" href="http://cells-uri/R4C3"/><gs:cell row="4" col="3" inputValue="4_3"/></entry>""")

  val changes1 = List(new URI("https://spreadsheets.google.com/feeds/cells/872A3CBF-0EEF-41FD-8D21-23E79614393A/od6/private/full/R4C1/5ckm"), new URI("https://spreadsheets.google.com/feeds/cells/872A3CBF-0EEF-41FD-8D21-23E79614393A/od6/private/full/R4C3/k4jc8"))

  val resp7 = {
    val ent = new StringEntity("""<entry xmlns="http://www.w3.org/2005/Atom"><id>https://spreadsheets.google.com/feeds/spreadsheets/D5498A33-8CB0-4AD9-84AD-996566CB7BFE</id><updated>2014-08-31T15:11:45.892Z</updated><category term="http://schemas.google.com/spreadsheets/2006#spreadsheet" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">Sheet 1</title><content type="text">Sheet 1</content><link href="https://spreadsheets.google.com/feeds/worksheets/D5498A33-8CB0-4AD9-84AD-996566CB7BFE/private/full" type="application/atom+xml" rel="http://schemas.google.com/spreadsheets/2006#worksheetsfeed"/><link href="https://docs.google.com/spreadsheets/d/D5498A33-8CB0-4AD9-84AD-996566CB7BFE/edit" type="text/html" rel="alternate"/><link href="https://spreadsheets.google.com/feeds/spreadsheets/D5498A33-8CB0-4AD9-84AD-996566CB7BFE" type="application/atom+xml" rel="self"/><author><name>google</name><email>email@google.com</email></author></entry>""")
    val resp = new BasicHttpResponse(new BasicStatusLine(httpProto, 200, "OK"))
    resp.setEntity(ent)
    resp
  }

  val infos3 = {
    val datefmt =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    SpreadsheetInfo(
      id = "D5498A33-8CB0-4AD9-84AD-996566CB7BFE",
      updated = datefmt.parse("2014-08-31T15:11:45.892Z"),
      title = "Sheet 1",
      selfUri = new URI("https://spreadsheets.google.com/feeds/spreadsheets/D5498A33-8CB0-4AD9-84AD-996566CB7BFE"),
      worksheetsUri = new URI("https://spreadsheets.google.com/feeds/worksheets/D5498A33-8CB0-4AD9-84AD-996566CB7BFE/private/full"))
  }

  def resp8 = (resp2 :: resp6.toList).iterator

  def body7 = List("""<entry xmlns="http://www.w3.org/2005/Atom" xmlns:gs="http://schemas.google.com/spreadsheets/2006"><id>https://spreadsheets.google.com/feeds/cells/178A6E9A-34EB-435E-B483-61E2169AEB79/od6/private/full/R4C1</id><link rel="edit" type="application/atom+xml" href="https://spreadsheets.google.com/feeds/cells/178A6E9A-34EB-435E-B483-61E2169AEB79/od6/private/full/R4C1"/><gs:cell row="4" col="1" inputValue="4_1"/></entry>""", """<entry xmlns="http://www.w3.org/2005/Atom" xmlns:gs="http://schemas.google.com/spreadsheets/2006"><id>https://spreadsheets.google.com/feeds/cells/178A6E9A-34EB-435E-B483-61E2169AEB79/od6/private/full/R4C3</id><link rel="edit" type="application/atom+xml" href="https://spreadsheets.google.com/feeds/cells/178A6E9A-34EB-435E-B483-61E2169AEB79/od6/private/full/R4C3"/><gs:cell row="4" col="3" inputValue="4_3"/></entry>""")

  def resp9 = List(resp2, resp4).iterator

  val body9 = List("""<entry xmlns="http://www.w3.org/2005/Atom" xmlns:gs="http://schemas.google.com/spreadsheets/2006"><id>https://spreadsheets.google.com/feeds/cells/_spreadsheetId3/od6/private/full/R4C1</id><link rel="edit" type="application/atom+xml" href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId3/od6/private/full/R4C1"/><gs:cell row="4" col="1" inputValue="4_1"/></entry>""", """<entry xmlns="http://www.w3.org/2005/Atom" xmlns:gs="http://schemas.google.com/spreadsheets/2006"><id>https://spreadsheets.google.com/feeds/cells/_spreadsheetId3/od6/private/full/R4C3</id><link rel="edit" type="application/atom+xml" href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId3/od6/private/full/R4C3"/><gs:cell row="4" col="3" inputValue="4_3"/></entry>""")

  val badRequest = new BasicHttpResponse(
    new BasicStatusLine(httpProto, 400, "Bad Request"))

  def resp10 = {
    val ent1 = new StringEntity("""<feed xmlns:batch="http://schemas.google.com/gdata/batch" xmlns:gs="http://schemas.google.com/spreadsheets/2006" xmlns:openSearch="http://a9.com/-/spec/opensearchrss/1.0/" xmlns="http://www.w3.org/2005/Atom"><id>https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">1.Sheet</title><link href="https://docs.google.com/spreadsheets/d/_spreadsheetId6/edit" type="application/atom+xml" rel="alternate"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#feed"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#post"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full/batch" type="application/atom+xml" rel="http://schemas.google.com/g/2005#batch"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full" type="application/atom+xml" rel="self"/><author><name>google</name><email>test@ema.il</email></author><openSearch:totalResults>5</openSearch:totalResults><openSearch:startIndex>1</openSearch:startIndex><gs:rowCount>1000</gs:rowCount><gs:colCount>26</gs:colCount><entry><id>https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full/R1C1</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">A1</title><content type="text">X</content><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full/R1C1" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full/R1C1/2g" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Val.1" col="1" row="1">Val.1</gs:cell></entry></feed>""")
    val respA = new BasicHttpResponse(new BasicStatusLine(httpProto, 200, "OK"))
    respA.setEntity(ent1)

    val ent2 = new StringEntity("""<feed xmlns:batch="http://schemas.google.com/gdata/batch" xmlns:gs="http://schemas.google.com/spreadsheets/2006" xmlns:openSearch="http://a9.com/-/spec/opensearchrss/1.0/" xmlns="http://www.w3.org/2005/Atom"><id>https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">1.Sheet</title><link href="https://docs.google.com/spreadsheets/d/_spreadsheetId6/edit" type="application/atom+xml" rel="alternate"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#feed"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#post"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full/batch" type="application/atom+xml" rel="http://schemas.google.com/g/2005#batch"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full" type="application/atom+xml" rel="self"/><author><name>google</name><email>test@ema.il</email></author><openSearch:totalResults>5</openSearch:totalResults><openSearch:startIndex>1</openSearch:startIndex><gs:rowCount>1000</gs:rowCount><gs:colCount>26</gs:colCount><entry><id>https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full/R3C1</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">A3</title><content type="text">Pouet</content><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full/R3C1" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full/R3C1/19y351" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Test 5" col="1" row="3">Test 5</gs:cell></entry></feed>""")
    val respB = new BasicHttpResponse(new BasicStatusLine(httpProto, 200, "OK"))
    respB.setEntity(ent2)

    val ent3 = new StringEntity("""<?xml version='1.0' encoding='UTF-8'?><entry xmlns='http://www.w3.org/2005/Atom' xmlns:gs='http://schemas.google.com/spreadsheets/2006' xmlns:batch='http://schemas.google.com/gdata/batch'><id>https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full/R4C1</id><updated>2014-08-28T18:32:54.266Z</updated><category scheme='http://schemas.google.com/spreadsheets/2006' term='http://schemas.google.com/spreadsheets/2006#cell'/><title type='text'>A4</title><content type='text'>appended</content><link rel='self' type='application/atom+xml' href='https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full/R4C1'/><link rel='edit' type='application/atom+xml' href='https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full/R4C1/7clm'/><gs:cell row='4' col='1' inputValue='appended'>appended</gs:cell></entry>""")

    val respC = new BasicHttpResponse(
      new BasicStatusLine(httpProto, 201, "Created"))

    respC.setEntity(ent3)

    List(respA, respB, respC).iterator
  }

  val body10 = """<entry xmlns="http://www.w3.org/2005/Atom" xmlns:gs="http://schemas.google.com/spreadsheets/2006"><id>https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full/R4C1</id><link rel="edit" type="application/atom+xml" href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId3/private/full/R4C1"/><gs:cell row="4" col="1" inputValue="appended"/></entry>"""

  def resp11 = {
    val ent1 = new StringEntity("""<feed xmlns:batch="http://schemas.google.com/gdata/batch" xmlns:gs="http://schemas.google.com/spreadsheets/2006" xmlns:openSearch="http://a9.com/-/spec/opensearchrss/1.0/" xmlns="http://www.w3.org/2005/Atom"><id>https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId2/private/full</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">1.Sheet</title><link href="https://docs.google.com/spreadsheets/d/_spreadsheetId6/edit" type="application/atom+xml" rel="alternate"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId2/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#feed"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId2/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#post"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId2/private/full/batch" type="application/atom+xml" rel="http://schemas.google.com/g/2005#batch"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId2/private/full" type="application/atom+xml" rel="self"/><author><name>google</name><email>test@ema.il</email></author><openSearch:totalResults>0</openSearch:totalResults><openSearch:startIndex>1</openSearch:startIndex><gs:rowCount>1000</gs:rowCount><gs:colCount>26</gs:colCount></feed>""")
    val respA = new BasicHttpResponse(new BasicStatusLine(httpProto, 200, "OK"))
    respA.setEntity(ent1)

    val ent2 = new StringEntity("""<feed xmlns:gs="http://schemas.google.com/spreadsheets/2006" xmlns:openSearch="http://a9.com/-/spec/opensearchrss/1.0/" xmlns="http://www.w3.org/2005/Atom"><id>https://spreadsheets.google.com/feeds/worksheets/_spreadsheetId6/private/full</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#worksheet" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">2.Sheet</title><link href="https://docs.google.com/spreadsheets/d/_spreadsheetId6/edit" type="application/atom+xml" rel="alternate"/><link href="https://spreadsheets.google.com/feeds/worksheets/_spreadsheetId6/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#feed"/><link href="https://spreadsheets.google.com/feeds/worksheets/_spreadsheetId6/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#post"/><link href="https://spreadsheets.google.com/feeds/worksheets/_spreadsheetId6/private/full" type="application/atom+xml" rel="self"/><author><name>google</name><email>test@ema.il</email></author><openSearch:totalResults>1</openSearch:totalResults><openSearch:startIndex>1</openSearch:startIndex><entry><id>https://spreadsheets.google.com/feeds/worksheets/_spreadsheetId6/private/full/_worksheetId2</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#worksheet" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">Worksheet 1</title><content type="text">Worksheet 1</content><link href="https://spreadsheets.google.com/feeds/list/_spreadsheetId6/_worksheetId2/private/full" type="application/atom+xml" rel="http://schemas.google.com/spreadsheets/2006#listfeed"/><link href="https://spreadsheets.google.com/feeds/cells/_spreadsheetId6/_worksheetId2/private/full" type="application/atom+xml" rel="http://schemas.google.com/spreadsheets/2006#cellsfeed"/><link href="https://docs.google.com/spreadsheets/d/_spreadsheetId6/gviz/tq?gid=0" type="application/atom+xml" rel="http://schemas.google.com/visualization/2008#visualizationApi"/><link href="https://docs.google.com/spreadsheets/d/_spreadsheetId6/export?gid=0&amp;format=csv" type="text/csv" rel="http://schemas.google.com/spreadsheets/2006#exportcsv"/><link href="https://spreadsheets.google.com/feeds/worksheets/_spreadsheetId6/private/full/_worksheetId2" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/worksheets/_spreadsheetId6/private/full/_worksheetId2/u9oqmj" type="application/atom+xml" rel="edit"/><gs:colCount>26</gs:colCount><gs:rowCount>1000</gs:rowCount></entry></feed>""")
    val respB = new BasicHttpResponse(new BasicStatusLine(httpProto, 200, "OK"))
    respB.setEntity(ent2)

    List(respA, respB).iterator
  }

  def resp12 = {
    val respA = new BasicHttpResponse(
      new BasicStatusLine(httpProto, 400, "Bad Request"))

    val respB = new BasicHttpResponse(
      new BasicStatusLine(httpProto, 404, "Not Found"))

    List(respA, respB).iterator
  }
}
