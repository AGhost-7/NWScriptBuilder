package aghost7.nwscriptbuilder

import java.nio.file._
import java.io.File
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import Console._
import scala.collection.mutable.{Map => MMap}
import scala.collection.JavaConversions._
import akka.actor.{ActorSystem,PoisonPill}


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
	
	val system = ActorSystem("NwScriptBuilder")
	
	implicit val tag = LoggerTag("")
	
	Logger.info("Initializing...")
	
	Logger.debug("Running in debug mode.")
	
	Logger.debug("Configuration file found: " + Conf.userFileFound)
	
	// Process configuration file...
	val conf = Conf.get
	
	val compiler = CompilerProcessor.fromConfig(conf.getConfig("compiler"))
	val compAll = conf.getBoolean("watchers.startup.full-compile")
	
	val tracker = system.actorOf(NssTracker.props(compiler))
	conf.getStringList("watchers.startup.directories").foreach { dir =>
		tracker ! WatchCmd(dir, compAll)
	}
	
	
	val argPat = """(["][\\A-z0-9\/ ]+["])|([ ]?[\\A-z0-9\/-]+[ ])|([\\A-z0-9\/-]+)""".r
	
	def tick = {
		print("> ")
		flush
	}
	
	def toClipboard(s: String) {
		val select = new StringSelection(s)
		Toolkit
			.getDefaultToolkit
			.getSystemClipboard
			.setContents(select, select)
	}
	
	/** Processes the arguments passed through the mini console mode.
	 */
	def processArgs(args: List[String]): Unit = args match {
		case Nil => 
			tick
			val input = readLine
			val consArgs = argPat.findAllMatchIn(input).map { _.toString.trim }
			if(consArgs.isEmpty) println("")
			processArgs(consArgs.toList)
			
		case "clear" :: rest =>
			tracker ! ClearWatch
			processArgs(rest)
			
		case "watch" :: Path(dir) :: rest =>
				val dirFile = new File(dir)
				if(dirFile.exists) {
					val path = dirFile.getAbsolutePath
					tracker ! WatchCmd(path, false)
				} else {
					Logger.error(s"target $dir does not exist.")
				}
			processArgs(rest)
			
		case "remove" :: Path(dir) :: rest =>
			val abs = new File(dir).getAbsolutePath
		//	watchers.remove(abs).fold[Unit] {
		//		Logger.error(s"No watch for directory $abs found.")
		//	} { listen =>
		//		listen.purge
		//		Logger.info("Watch succesfully removed")
		//	}
			processArgs(rest)
			
		case "all" :: rest =>
			println("")
		//	watchers.values.foreach { w => compiler.compileAll(w.dirName) }
			processArgs(rest)
		
		case "chars" :: rest =>
		//	val names = watchers.values.flatMap { w => w.fileNames }.toList
			
		/*	val statsStr = Stats.charCount(names)
				.map { case (char, count) => char + " = " + count }
				.mkString("\n")
				
			Logger.info("-Character stats for watched files-\n" + statsStr)*/
			
			processArgs(rest)
			
		case "chars-recommend" :: n :: rest if(n.forall{ _.isDigit }) =>
		/*	val names = watchers.values.flatMap {w => w.fileNames}.toList
			val stats = Stats
				.recommendChars(names, n.toInt)
			val charCombos = stats
				.map { charset => "\"" + charset + "\"" }
				.mkString(", ")
				
			toClipboard("[" + charCombos + "]")
			Logger.info("Recommended combination: " + stats.mkString(", "))*/
			
		case "exit" :: rest =>
			
		//	watchers.values.foreach { _.purge }
			tracker ! PoisonPill
			system.awaitTermination()
			Logger.info("Exiting...")
			
			// Application shuts down naturally (no System.exit(0))
		
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


