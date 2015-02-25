package aghost7.nwscriptbuilder

import java.nio.file._
import java.io.File

import Console._
import scala.collection.mutable.{Map => MMap}
import scala.collection.JavaConversions._

import com.typesafe.config._


/** Extracts full path string from arguments in pattern match.
 */
object Path {
	def unapply(p: String): Option[String] = 
		if(p.head == '\"' && p.last == '\"')
			Some(p.tail.dropRight(1))
		else 
			Some(p)
}

/** Application main, responsible for parsing arguments.
 */
object Main extends App {
	
	val watchers = MMap[String, FileSystemWatcher]()
	
	implicit val tag = LoggerTag("")
	
	Logger.info("Initializing...")
	
	Logger.debug("Running in debug mode.")
	
	// Process configuration file...
	val conf = {
		val jarPath = getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath
		val jar = new File(jarPath)
		val path = jar.getParentFile().getAbsolutePath() + "/application.conf"
		val userFile = new File(path)
		Logger.debug("Configuration file found: " + userFile.exists)
		val userConf = ConfigFactory.parseFile(userFile)
		ConfigFactory.load(userConf) 
	}
	
	val compiler = CompilerProcessor.fromConfig(conf.getConfig("compiler"))
	val compAll = conf.getBoolean("watchers.startup.full-compile")
	
	conf.getStringList("watchers.startup.directories").foreach { dir =>
		watchers += dir -> new FileSystemWatcher(dir, compiler, compAll)
	}
	
	val argPat = """(["][\\A-z0-9\/ ]+["])|([ ]?[\\A-z0-9\/-]+[ ])|([\\A-z0-9\/-]+)""".r
	
	/** Processes the arguments passed through the mini console mode.
	 */
	def processArgs(args: List[String]): Unit = args match {
		case Nil => 
			val input = readLine
			println("")
			val consArgs = argPat.findAllMatchIn(input).map { _.toString.trim }
			processArgs(consArgs.toList)
		case "clear" :: rest =>
			watchers.values.foreach { _.purge }
			watchers.clear
			processArgs(rest)
		case "watch" :: Path(dir) :: rest =>
			if(watchers.get(dir).isDefined){
				Logger.error(s"watch is already active for directory $dir")
			} else {
				val dirFile = new File(dir)
				if(dirFile.exists) {
					val path = dirFile.getAbsolutePath
					watchers += dir -> new FileSystemWatcher(path, compiler, false)
				} else {
					Logger.error(s"target $dir does not exist.")
				}
					
			}
			processArgs(rest)
		case "remove" :: Path(dir) :: rest =>
			watchers.remove(dir).fold[Unit] {
				Logger.error(s"No watch for directory $dir found.")
			} { listen =>
				listen.purge
				Logger.info("Watch succesfully removed")
			}
			processArgs(rest)
		case "all" :: rest =>
			watchers.values.foreach { w => compiler.compileAll(w.dirName) }
			processArgs(rest)
		case "exit" :: rest =>
			watchers.values.foreach { _.purge }
			Logger.info("Exiting...")
		
		case skip :: rest =>
			Logger.info("argument not processed: " + skip)
			processArgs(rest)
	}
	
	try
		processArgs(Nil)
	finally 
		if(Logger.isDebug)
			Logger.writer.close()
		

}


