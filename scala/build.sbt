name := "scala"

javaOptions += "-deprecation"

resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"

libraryDependencies ++= Seq(
  "foorgol" % "java-client" % version.value,
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
  "com.jsuereth" %% "scala-arm" % "2.0",
  "org.specs2" %% "specs2-core" % "4.2.0" % Test)

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
