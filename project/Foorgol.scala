import sbt._
import Keys._

object Foorgol extends Build {
  lazy val java = Project(id = "java-client", base = file("java-client"))

  lazy val root = Project(id = "foorgol", base = file(".")).aggregate(java)
}
