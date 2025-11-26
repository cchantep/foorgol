lazy val java = Project(id = "java-client", base = file("java-client"))

lazy val scala = Project(id = "scala", base = file("scala")).dependsOn(java)

lazy val root = Project(id = "foorgol", base = file(".")).
  settings(
    organization in ThisBuild := "foorgol",
    version in ThisBuild := "1.0.6",
    scalaVersion in ThisBuild := "2.13.18",
    crossScalaVersions in ThisBuild := Seq("2.10.7", "2.11.12", scalaVersion.value)
  ).aggregate(java, scala)
