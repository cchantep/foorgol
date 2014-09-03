# Foorgol

Google API client (or one the Discworld, the Ephebian God of Avalanches).

[![Build Status](https://secure.travis-ci.org/applicius/foorgol.png?branch=master)](http://travis-ci.org/applicius/foorgol)

## Motivation

Google offer some nice Web API (Drive, Spreadsheet, ...), but Java client is not so nice to be easily integrated in a backend project (dependency issue, complexity).

Foorgol help integration of some of these features.

## Requirements

* Java 1.6+
* SBT 0.12+

## Usage

Foorgool can be used in SBT projects adding dependency `"fr.applicius.foorgol" %% "java-client" % "VERSION"` 
and having `"Applicius Snapshots" at "https://raw.github.com/applicius/mvn-repo/master/snapshots/"` in resolvers.

## Build

Foorgol can be built from these sources using SBT (0.12.2+): `sbt publish`
