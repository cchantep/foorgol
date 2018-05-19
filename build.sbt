lazy val java = Project(id = "java-client", base = file("java-client"))

lazy val scala = Project(id = "scala", base = file("scala")).dependsOn(java)

lazy val root = Project(id = "foorgol", base = file(".")).
  settings(
    organization in ThisBuild := "foorgol",
    version in ThisBuild := "1.0.6",
    scalaVersion in ThisBuild := "2.12.6",
    crossScalaVersions in ThisBuild := Seq("2.10.4", "2.11.3", "2.11.8")
  ).aggregate(java, scala)
