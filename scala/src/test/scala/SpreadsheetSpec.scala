package fr.applicius.foorgol

object SpreadsheetSpec
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

  "Worksheet list" should {
    "be successfully fetched" in {
      val mock = MockSpreadsheet("accessToken2", None) {
        MockHttpClient(_ ⇒ resp2)
      }

      mock.worksheets("http://worksheets-url").
        aka("list") must beEqualTo(infos2).await
    }
  }

  "Cells list" should {
    "be successfully fetched" in {
      val mock = MockSpreadsheet("accessToken3", None) {
        MockHttpClient(_ ⇒ resp3)
      }

      mock.cells("http://cells-url") aka "list" must beEqualTo(cells1).await
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
  import java.util.Locale
  import java.text.SimpleDateFormat

  import org.apache.http.ProtocolVersion
  import org.apache.http.entity.StringEntity
  import org.apache.http.message.{ BasicHttpResponse, BasicStatusLine }

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
        selfUrl = "https://spreadsheets.google.com/feeds/spreadsheets/private/full/D5498A33-8CB0-4AD9-84AD-996566CB7BFE",
        worksheetsUrl = "https://spreadsheets.google.com/feeds/worksheets/D5498A33-8CB0-4AD9-84AD-996566CB7BFE/private/full"),
      SpreadsheetInfo(
        id = "17vQxA3Yqsuu9knmahflx6LW60JO2_VMaZPy62zx5Bz8",
        updated = datefmt.parse("2014-08-28T18:32:54.299Z"),
        title = "2.Sheet",
        selfUrl = "https://spreadsheets.google.com/feeds/spreadsheets/private/full/17vQxA3Yqsuu9knmahflx6LW60JO2_VMaZPy62zx5Bz8",
        worksheetsUrl = "https://spreadsheets.google.com/feeds/worksheets/17vQxA3Yqsuu9knmahflx6LW60JO2_VMaZPy62zx5Bz8/private/full"))
  }

  val resp2 = {
    val ent = new StringEntity("""<feed xmlns:gs="http://schemas.google.com/spreadsheets/2006" xmlns:openSearch="http://a9.com/-/spec/opensearchrss/1.0/" xmlns="http://www.w3.org/2005/Atom"><id>https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#worksheet" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">2.Sheet</title><link href="https://docs.google.com/spreadsheets/d/178A6E9A-34EB-435E-B483-61E2169AEB79/edit" type="application/atom+xml" rel="alternate"/><link href="https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#feed"/><link href="https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#post"/><link href="https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full" type="application/atom+xml" rel="self"/><author><name>google</name><email>google@applicius.fr</email></author><openSearch:totalResults>1</openSearch:totalResults><openSearch:startIndex>1</openSearch:startIndex><entry><id>https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full/od6</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#worksheet" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">Worksheet 1</title><content type="text">Worksheet 1</content><link href="https://spreadsheets.google.com/feeds/list/178A6E9A-34EB-435E-B483-61E2169AEB79/od6/private/full" type="application/atom+xml" rel="http://schemas.google.com/spreadsheets/2006#listfeed"/><link href="https://spreadsheets.google.com/feeds/cells/178A6E9A-34EB-435E-B483-61E2169AEB79/od6/private/full" type="application/atom+xml" rel="http://schemas.google.com/spreadsheets/2006#cellsfeed"/><link href="https://docs.google.com/spreadsheets/d/178A6E9A-34EB-435E-B483-61E2169AEB79/gviz/tq?gid=0" type="application/atom+xml" rel="http://schemas.google.com/visualization/2008#visualizationApi"/><link href="https://docs.google.com/spreadsheets/d/178A6E9A-34EB-435E-B483-61E2169AEB79/export?gid=0&amp;format=csv" type="text/csv" rel="http://schemas.google.com/spreadsheets/2006#exportcsv"/><link href="https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full/od6" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full/od6/u9oqmj" type="application/atom+xml" rel="edit"/><gs:colCount>26</gs:colCount><gs:rowCount>1000</gs:rowCount></entry></feed>""")
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
      selfUrl = "https://spreadsheets.google.com/feeds/worksheets/178A6E9A-34EB-435E-B483-61E2169AEB79/private/full/od6",
      cellsUrl = "https://spreadsheets.google.com/feeds/cells/178A6E9A-34EB-435E-B483-61E2169AEB79/od6/private/full"))
  }

  val resp3 = {
    val ent = new StringEntity("""<feed xmlns:batch="http://schemas.google.com/gdata/batch" xmlns:gs="http://schemas.google.com/spreadsheets/2006" xmlns:openSearch="http://a9.com/-/spec/opensearchrss/1.0/" xmlns="http://www.w3.org/2005/Atom"><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">1.Sheet</title><link href="https://docs.google.com/spreadsheets/d/8F53A111-4351-4E7E-A460-51D31F03D599/edit" type="application/atom+xml" rel="alternate"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#feed"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full" type="application/atom+xml" rel="http://schemas.google.com/g/2005#post"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/batch" type="application/atom+xml" rel="http://schemas.google.com/g/2005#batch"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full" type="application/atom+xml" rel="self"/><author><name>google</name><email>google@applicius.fr</email></author><openSearch:totalResults>5</openSearch:totalResults><openSearch:startIndex>1</openSearch:startIndex><gs:rowCount>1000</gs:rowCount><gs:colCount>26</gs:colCount><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">A1</title><content type="text">X</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1/2g" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Val.1" col="1" row="1">Val.1</gs:cell></entry><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">B1</title><content type="text">Y</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2/9zlg9" type="application/atom+xml" rel="edit"/><gs:cell inputValue="2# Val" col="2" row="1">2# Val</gs:cell></entry><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">A2</title><content type="text">Yoyo</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1/1olos" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Test" col="1" row="2">Test</gs:cell></entry><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">B2</title><content type="text">Kaka</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2/bc5e4" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Value 4" col="2" row="2">Value 4</gs:cell></entry><entry><id>https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1</id><updated>2014-08-28T18:32:54.266Z</updated><category term="http://schemas.google.com/spreadsheets/2006#cell" scheme="http://schemas.google.com/spreadsheets/2006"/><title type="text">A3</title><content type="text">Pouet</content><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1" type="application/atom+xml" rel="self"/><link href="https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1/19y351" type="application/atom+xml" rel="edit"/><gs:cell inputValue="Test 5" col="1" row="3">Test 5</gs:cell></entry></feed>""")
    val resp = new BasicHttpResponse(new BasicStatusLine(httpProto, 200, "OK"))
    resp.setEntity(ent)
    resp
  }

  val cells1 = WorksheetCells(
    totalCount = 5,
    batchUrl = "https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/batch",
    cells = List(
      WorksheetCell(
        id = "R1C1", title = "A1",
        selfUrl = "https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1",
        editUrl = "https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C1/2g",
        row = 1, col = 1,
        value = "Val.1"),
      WorksheetCell(
        id = "R1C2", title = "B1",
        selfUrl = "https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2",
        editUrl = "https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R1C2/9zlg9",
        row = 1, col = 2,
        value = "2# Val"),
      WorksheetCell(
        id = "R2C1", title = "A2",
        selfUrl = "https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1",
        editUrl = "https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C1/1olos",
        row = 2, col = 1,
        value = "Test"),
      WorksheetCell(
        id = "R2C2", title = "B2",
        selfUrl = "https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2",
        editUrl = "https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R2C2/bc5e4",
        row = 2, col = 2,
        value = "Value 4"),
      WorksheetCell(
        id = "R3C1", title = "A3",
        selfUrl = "https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1",
        editUrl = "https://spreadsheets.google.com/feeds/cells/8F53A111-4351-4E7E-A460-51D31F03D599/od6/private/full/R3C1/19y351",
        row = 3, col = 1,
        value = "Test 5")))
}
