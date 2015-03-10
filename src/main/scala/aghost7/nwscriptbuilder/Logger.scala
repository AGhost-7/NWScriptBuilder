package aghost7.nwscriptbuilder

import java.io.{PrintWriter,BufferedWriter,FileWriter}
import java.io.File

case class LoggerTag(name: String)

object Logger {
	
	val debugProp = System.getProperty("aghost7.nwscriptbuilder.debug")
	val isDebug = "true".equalsIgnoreCase(debugProp)
	
	val isPrinting = Conf.get.getBoolean("log-to-file")
	
	
	lazy val writer = {
		val jarPath = getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath
		val jar = new File(jarPath)
		val path = jar.getParentFile().getAbsolutePath() + "/log.txt"
		val file = new File(path)
		if(!file.exists()) file.createNewFile()
		new PrintWriter(new BufferedWriter(new FileWriter(file, true)))
	}
	
	def debug(s: String, newLine: Boolean = true)(implicit tag: LoggerTag) {
		if(isDebug){
			if(newLine) println("")
			println(tag.name + s)
			writer.println(tag.name + s)
		}
	}
	
	def info(s: String, newLine: Boolean = true)(implicit tag: LoggerTag) {
		if(newLine) println("")
		println(tag.name + s)
		if(isDebug || isPrinting) {
			writer.println(tag.name + s)
		}
	}
	
	def error(s: String, newLine: Boolean = true)(implicit tag: LoggerTag){
		if(newLine) println("")
		println(tag.name + s)
		if(isDebug || isPrinting){
			writer.println(tag.name + s)
		}
	}
	
	
	
}