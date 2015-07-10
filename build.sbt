name := "NwScript Builder"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
	"com.typesafe" % "config" % "1.2.1",
	"org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
	"junit" % "junit" % "4.8.1" % "test",
	"com.typesafe.akka" % "akka-actor_2.11" % "2.3.9"
	)


mainClass := Some("aghost7.nwscriptbuilder.Main")

assemblyJarName in assembly := "NWScriptBuilder.jar"

lazy val wrapUp = taskKey[File](
	"Puts everything together for uploading application to websites.")

wrapUp := {
	// List assets
	val jar = assembly.value
	val bat = file("scripts/NWScriptBuilder.bat")
	val shell = file("scripts/NWScriptBuilder")
	val ref = file("src/main/resources/reference.conf")
	val readme = file("readme.md")

	// transforms
	val out = target.value / "dist.zip"

	val inputs = Seq(
			shell -> "NWScriptBuilder",
			bat -> "NWScriptBuilder.bat",
			jar -> "_NWScriptBuilder/NWScriptBuilder.jar",
			ref -> "_NWScriptBuilder/application.conf",
			readme -> "_NWScriptBuilder/readme.md")

	IO.zip(inputs, out)

	IO.delete(Seq(jar))

	out
}
