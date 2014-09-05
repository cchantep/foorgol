# Foorgol

Google API client (or one the Discworld, the Ephebian God of Avalanches).

[![Build Status](https://secure.travis-ci.org/applicius/foorgol.png?branch=master)](http://travis-ci.org/applicius/foorgol)

## Motivation

Google offer some nice Web API (Drive, Spreadsheet, ...), but Java client is not so nice to be easily integrated in a backend project (dependency issue, complexity).

Foorgol help integration of some of these features.

## Usage

Foorgool can be used in SBT projects adding dependency `"fr.applicius.foorgol" % "java-client" % "1.0-SNAPSHOT"` or `"fr.applicius.foorgol" %% "foorgol-scala" % "1.0-SNAPSHOT"` and having `"Applicius Snapshots" at "https://raw.github.com/applicius/mvn-repo/master/snapshots/"` in resolvers.

### Spreadsheet

Scala DSL for Google Spreadsheet can be initialized as following.

```scala
import fr.applicius.foorgol.{ RefreshToken, Spreadsheet }

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
import fr.applicius.foorgol.SpreadsheetInfo

// api: fr.applicius.foorgol.Spreadsheet

val spreadsheets: Future[List[SpreadsheetInfo]] = api.list
```

**Find a single spreadsheet by ID**

```scala
import scala.concurrent.Future
import fr.applicius.foorgol.SpreadsheetInfo

// api: fr.applicius.foorgol.Spreadsheet

val spreadsheet: Future[SpreadsheetInfo] = api.spreadsheet("anID")
```

**List all worksheets for a spreadsheet**

```scala
import scala.concurrent.Future
import fr.applicius.foorgol.WorksheetInfo

// api: fr.applicius.foorgol.Spreadsheet
// sheet: fr.applicius.foorgol.SpreadsheetInfo

val worksheets: Future[List[WorksheetInfo]] = 
  api.worksheets(sheet.worksheetsUrl)
```

**Find a single worksheet by spreadsheet ID and index**

```scala
import scala.concurrent.Future
import fr.applicius.foorgol.WorksheetInfo

// api: fr.applicius.foorgol.Spreadsheet
val firstWorksheet: Future[Option[WorksheetInfo]] = 
  api.worksheet("spreadsheetId", 0)
```

**Custom process with worksheets of a specified spreadsheet**

```scala
import scala.concurrent.Future
import fr.applicius.foorgol.WorksheetInfo

// api: fr.applicius.foorgol.Spreadsheet

// Find worksheet with matching title
val matching: Future[Option[WorksheetInfo]] =
  api.worksheet("spreadsheetId")(None: Option[WorksheetInfo]) { 
    case (_, w @ WorksheetInfo(_, _, "Matching title", _, _)) => Left(Some(w)) 
      // found some matching sheet, put it at final value in the `Left`
    case (st, _) => Right(st)
      // Not matching title so will look at other worksheets
  }
```

**List cells by URI**

```scala
import scala.concurrent.Future
import fr.applicius.foorgol.WorksheetCells

// api: fr.applicius.foorgol.Spreadsheet
// work: fr.applicius.foorgol.WorksheetInfo

val cells: Future[WorksheetCells] = api.cells(work.cellsUrl, None, None)
```

**List cells by spreadsheet ID and worksheet index**

```scala
import scala.concurrent.Future
import fr.applicius.foorgol.WorksheetCells

// api: fr.applicius.foorgol.Spreadsheet

val cells: Future[WorksheetCells] = api.cells("spreadsheetId", 0)
// All cells of first worksheet from specified spreadsheet
```

**Changing cells content by URI**

```scala
import scala.concurrent.Future

// api: fr.applicius.foorgol.Spreadsheet
// work: fr.applicius.foorgol.WorksheetInfo

// Will change content for cells (4, 1) and (4, 3)
val versionUrls: Future[List[String]] = api.change(work.cellsUrl, 
  List(CellValue(4, 1, "4_1"), CellValue(4, 3, "4_3")))
// These urls can be used for batch update
```

**Changing cells by spreadsheet ID and worksheet index**

```scala
import scala.concurrent.Future

// api: fr.applicius.foorgol.Spreadsheet

// Will change content for cells (1, 1) and (1, 2),
// in second worksheet of specified spreadsheet.
val versionUrls: Future[List[String]] = api.change("spreadsheetId", 1, 
  List(CellValue(1, 1, "1_1"), CellValue(1, 2, "1_2")))
```

## Requirements

* Java 1.6+
* SBT 0.12+

## Build

Foorgol can be built from these sources using SBT (0.12.2+): `sbt publish`
