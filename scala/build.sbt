name := "foorgol-scala"

javaOptions += "-deprecation"

resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"

libraryDependencies ++= Seq(
  "fr.applicius" % "foorgol-java" % version.value,
  "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.2",
  "com.jsuereth" % "scala-arm_2.11" % "1.4",
  "org.specs2" %% "specs2" % "2.4.1" % "test")

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
