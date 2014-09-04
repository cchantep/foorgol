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

**List all worksheets for a spreadsheet**

```scala
import scala.concurrent.Future
import fr.applicius.foorgol.WorksheetInfo

// api: fr.applicius.foorgol.Spreadsheet
// sheet: fr.applicius.foorgol.SpreadsheetInfo

val worksheets: Future[List[WorksheetInfo]] = 
  api.worksheets(sheet.worksheetsUrl)
```

## Requirements

* Java 1.6+
* SBT 0.12+

## Build

Foorgol can be built from these sources using SBT (0.12.2+): `sbt publish`
