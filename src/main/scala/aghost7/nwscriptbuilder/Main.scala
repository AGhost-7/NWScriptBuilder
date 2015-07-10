package aghost7.nwscriptbuilder

import java.nio.file._
import java.io.File

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
object Main extends App with UXControls {
	
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
	conf
		.getStringList("watchers.startup.directories")
		.foreach { tracker ! WatchCmd(_, compAll) }
	
	
	val argPat = """(["][\\A-z0-9\/ ]+["])|([ ]?[\\A-z0-9\/-]+[ ])|([\\A-z0-9\/-]+)""".r
	
	/** Processes the arguments passed through the mini console mode. */
	def processArgs(args: List[String]): Unit = args match {
		case Nil => 
			val input = readLine
			val consArgs = argPat.findAllMatchIn(input).map { _.toString.trim }
			if(consArgs.isEmpty) {
				println("")
				tick
			}
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
				tick
			}
			processArgs(rest)
			
		case "remove" :: Path(dir) :: rest =>
			val abs = new File(dir).getAbsolutePath
			tracker ! RemoveCmd(abs)
			processArgs(rest)
			
		case "all" :: rest =>
			println("")
			tracker ! CompAllCmd
			processArgs(rest)
		
		case "chars" :: rest =>
			tracker ! CharsCountCmd
			processArgs(rest)
			
		case "chars-recommend" :: n :: rest if(n.forall{ _.isDigit }) =>
			tracker ! CharsRecommendCmd(n.toInt)
			processArgs(rest)
			
		case "exit" :: rest =>
			tracker ! PoisonPill
			system.shutdown()
			Logger.info("Exiting...")
			
			// Application shuts down naturally (no System.exit(0))
		
		case skip :: rest =>
			Logger.info("argument not processed: " + skip)
			processArgs(rest)
	}
	
	try
		processArgs(Nil)
	finally 
		if(Logger.isDebug || Logger.isPrinting)
			Logger.writer.close()
		

}


