# Foorgol

Google API client (or one the Discworld, the Ephebian God of Avalanches).

[![Build Status](https://secure.travis-ci.org/cchantep/foorgol.png?branch=master)](http://travis-ci.org/cchantep/foorgol)

## Motivation

Google offer some nice Web API (Drive, Spreadsheet, ...), but Java client is not so nice to be easily integrated in a backend project (dependency issue, complexity).

Foorgol help integration of some of these features.

## Usage

Foorgool can be used in SBT projects adding dependency `"foorgol" % "java-client" % "1.0.5-SNAPSHOT"` or `"foorgol" %% "scala" % "1.0.5-SNAPSHOT"` and having `"Tatami Releases" at "https://raw.github.com/cchantep/tatami/master/releases/"` in resolvers.

* Low-level [Java API](http://cchantep.github.io/foorgol/java-client/api/)
* [Scala API](http://cchantep.github.io/foorgol/scala/api/#package)

### Spreadsheet

Scala [DSL for Google Spreadsheet](http://cchantep.github.io/foorgol/scala/api/#foorgol.Spreadsheet) can be initialized as following.

```scala
import foorgol.{ RefreshToken, Spreadsheet }

val api: Spreadsheet = Spreadsheet("accessToken")
// Given access token must not be expired, as there refresh token is 
// not provided along, so it can be refreshed automatically.

val refreshableApi: Spreadsheet = Spreadsheet("maybeExpiredAccessToken",
  Some(RefreshToken("clientId", "clientSecret", "refreshToken")))
// If access token is expired, then if will be automatically refreshed
```

Once access to Google Spreadsheet is initialized, following features can be used.

**List all available spreadsheets**

```scala
import scala.concurrent.Future
import foorgol.SpreadsheetInfo

// api: foorgol.Spreadsheet

val spreadsheets: Future[List[SpreadsheetInfo]] = api.list
```

**Find a single spreadsheet by ID**

```scala
import scala.concurrent.Future
import foorgol.SpreadsheetInfo

// api: foorgol.Spreadsheet

val spreadsheet: Future[SpreadsheetInfo] = api.spreadsheet("anID")
```

**Create worksheet**

```scala
import scala.concurrent.Future

// api: foorgol.Spreadsheet

val id: Future[String] = api.createWorksheet(spreadsheetId, "Work title")
```

**List worksheets by spreadsheet ID**

```scala
import scala.concurrent.Future
import foorgol.WorksheetInfo

// api: foorgol.Spreadsheet

val worksheets: Future[List[WorksheetInfo]] = api.worksheets(spreadsheetId)
```

**List worksheets by URI**

```scala
import scala.concurrent.Future
import foorgol.WorksheetInfo

// api: foorgol.Spreadsheet
// sheet: foorgol.SpreadsheetInfo

val worksheets: Future[List[WorksheetInfo]] = 
  api.worksheets(sheet.worksheetsUri)
```

**Find a single worksheet by spreadsheet ID and worksheet index**

```scala
import scala.concurrent.Future
import foorgol.WorksheetInfo

// api: foorgol.Spreadsheet
val firstWorksheet: Future[Option[WorksheetInfo]] = 
  api.worksheet("spreadsheetId", 0)
```

**Find a single worksheet by spreadsheet ID and worksheet ID**

```scala
import scala.concurrent.Future
import foorgol.WorksheetInfo

// api: foorgol.Spreadsheet
val worksheet: Future[Option[WorksheetInfo]] = 
  api.worksheet("spreadsheetId", "worksheetId")
```

**Custom process with worksheets of a specified spreadsheet**

```scala
import scala.concurrent.Future
import foorgol.WorksheetInfo

// api: foorgol.Spreadsheet

// Find worksheet with matching title
val matching: Future[Option[WorksheetInfo]] =
  api.worksheet("spreadsheetId")(None: Option[WorksheetInfo]) { 
    case (_, w @ WorksheetInfo(_, _, "Matching title", _, _)) => Left(Some(w)) 
      // found some matching sheet, put it at final value in the `Left`
    case (st, _) => Right(st)
      // Not matching title so will look at other worksheets
  }
```

**Read cells by spreadsheet ID and worksheet index**

```scala
import scala.concurrent.Future
import foorgol.WorksheetCells

// api: foorgol.Spreadsheet

val cells: Future[Option[WorksheetCells]] = 
  api.cells("spreadsheetId", 0, None, None)
// All cells of first worksheet from specified spreadsheet
```

**Read cells by spreadsheet ID and worksheet ID**

```scala
import scala.concurrent.Future
import foorgol.WorksheetCells

// api: foorgol.Spreadsheet

val cells: Future[Option[WorksheetCells]] = 
  api.cells("spreadsheetId", "worksheetId", None, None)
// All cells of specified worksheet
```

**Read cells by URI**

```scala
import scala.concurrent.Future
import foorgol.WorksheetCells

// api: foorgol.Spreadsheet
// work: foorgol.WorksheetInfo

val cells: Future[Option[WorksheetCells]] = api.cells(work.cellsUri, None, None)
```

**Get last row by spreadsheet ID and worksheet ID**

```scala
import scala.concurrent.Future
import foorgol.WorksheetCells

// api: foorgol.Spreadsheet

val last: Future[Option[WorksheetCells]] = 
  api.lastRow("spreadsheetId", "worksheetId")
```

**Changing cells content by URI**

```scala
import scala.concurrent.Future

// api: foorgol.Spreadsheet
// work: foorgol.WorksheetInfo

// Will change content for cells (4, 1) and (4, 3)
val versionUris: Future[List[String]] = api.change(work.cellsUri, 
  List(CellValue(4, 1, "4_1"), CellValue(4, 3, "4_3")))
// These urls can be used for batch update
```

**Changing cells by spreadsheet ID and worksheet index**

```scala
import scala.concurrent.Future

// api: foorgol.Spreadsheet

// Will change content for cells (1, 1) and (1, 2),
// in second worksheet of specified spreadsheet.
val versionUris: Future[List[String]] = api.change("spreadsheetId", 1, 
  List(CellValue(1, 1, "1_1"), CellValue(1, 2, "1_2")))
```

**Changing cells by spreadsheet ID and worksheet ID**

```scala
import scala.concurrent.Future

// api: foorgol.Spreadsheet

// Will change content for cells (1, 1) and (1, 2),
// in specified worksheet of specified spreadsheet.
val versionUris: Future[List[String]] = 
  api.change("spreadsheetId", "worksheetId", 
    List(CellValue(1, 1, "1_1"), CellValue(1, 2, "1_2")))
```

**Append cells at end of specified worksheet**

```scala
import scala.concurrent.Future

// api: foorgol.Spreadsheet

// Append a row with first cell "A" and third one "C"
val versionUris: Future[List[String]] = 
  api.append("spreadsheetId", "worksheetId", List(
    1 -> "A", 3 -> "C"))
```

**Append row at end of specified worksheet**

```scala
import scala.concurrent.Future

// api: foorgol.Spreadsheet

// Append a row with contiguous cells ("A", "B", "C")
val versionUris: Future[List[String]] = 
  api.append("spreadsheetId", "worksheetId", "A", "B", "C")
```

> Given values are assumed to be contiguous.
> Other `.append(spreadsheetId: String, worksheetId: String, values: List[(Int, String)])` must be prefered.

## Requirements

* Java 1.6+
* SBT 0.12+

## Build

Foorgol can be built from these sources using SBT (0.12.2+): `sbt publish`
