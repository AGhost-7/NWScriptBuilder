name := "NwScript Builder"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
	"com.typesafe" % "config" % "1.2.1",
	"org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
	"junit" % "junit" % "4.8.1" % "test"
	//"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
	//"ch.qos.logback" % "logback-classic" % "1.1.2"

	)

mainClass := Some("aghost7.nwscriptbuilder.Main")
