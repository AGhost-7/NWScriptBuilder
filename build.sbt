name := "NwScript Builder"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
	"com.typesafe" % "config" % "1.2.1",
	"org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
	"junit" % "junit" % "4.8.1" % "test"
	)

mainClass := Some("aghost7.nwscriptbuilder.Main")

assemblyJarName in assembly := "NWScriptBuilder.jar"

lazy val wrapUp = taskKey[File](
	"Puts everything together for uploading application to websites.")

wrapUp := {
	val jar = assembly.value
	val out = target.value / "download.zip"
	val script = file("scripts/NWScriptBuilder.bat")
	val ref = file("src/main/resources/reference.conf")
	val conf = target.value / "application.conf"
	val readme = file("readme.md")
	
	IO.copyFile(ref, conf)
	
	val inputs = Seq(jar, script, conf, readme) x Path.flat
	IO.zip(inputs, out)
	
	IO.delete(Seq(jar, conf))
	
	out
}