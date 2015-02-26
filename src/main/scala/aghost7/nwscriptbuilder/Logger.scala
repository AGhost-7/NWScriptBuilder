package aghost7.nwscriptbuilder

import java.io.{PrintWriter,BufferedWriter,FileWriter}
import java.io.File

case class LoggerTag(name: String)

object Logger {
	
	val debugProp = System.getProperty("aghost7.nwscriptbuilder.debug")
	val isDebug = "true".equalsIgnoreCase(debugProp)
	
	lazy val writer = {
		val jarPath = getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath
		val jar = new File(jarPath)
		val path = jar.getParentFile().getAbsolutePath() + "/log.txt"
		val file = new File(path)
		if(!file.exists()) file.createNewFile()
		new PrintWriter(new BufferedWriter(new FileWriter(file, true)))
	}
	
	type LoggerPrepend = Option[LoggerTag]
	
	def debug(s: String)(implicit tag: LoggerTag) {
		if(isDebug){
			println(tag.name + s)
			writer.println(tag.name + s)
		}
	}
	
	def info(s: String)(implicit tag: LoggerTag) {
		println(tag.name + s)
		if(isDebug) {
			writer.println(tag.name + s)
		}
	}
	
	def error(s: String)(implicit tag: LoggerTag){
		println(tag.name + s)
		if(isDebug){
			writer.println(tag.name + s)
		}
	}
	
	
	
}