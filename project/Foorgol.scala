import sbt._
import Keys._

object Foorgol extends Build {
  lazy val java = Project(id = "java-client", base = file("java-client"))

  lazy val scala = Project(id = "scala", base = file("scala")).dependsOn(java)

  lazy val root = Project(id = "foorgol", base = file(".")).
    settings(
      organization in ThisBuild := "fr.applicius.foorgol",
      version in ThisBuild := "1.0.1-SNAPSHOT",
      scalaVersion in ThisBuild := "2.11.2").
    aggregate(java, scala)
}
