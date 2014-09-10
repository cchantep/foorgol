name := "java-client"

javaOptions += "-deprecation"

crossPaths := false

resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-lang3" % "3.1",
  "org.apache.httpcomponents" % "httpclient" % "4.3.5",
  "com.google.code.gson" % "gson" % "2.3",
  "org.specs2" %% "specs2" % "2.4.1" % "test")

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
