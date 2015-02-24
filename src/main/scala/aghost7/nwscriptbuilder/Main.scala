package aghost7.nwscriptbuilder

import java.nio.file._
import Console._
import com.typesafe.config._
import scala.collection.mutable.{Map => MMap}
import scala.collection.JavaConversions._
import java.io.File

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
	
	println("Initializing...")
	
	// Process configuration file...
	val conf = {
		val userFile = new File("application.conf")
		println("Configuration file found: " + userFile.exists())
		val userConf = ConfigFactory.parseFile(userFile)
		ConfigFactory.load(userConf) 
	}	
	
	val compiler = CompilerProcessor.fromConfig(conf.getConfig("compiler"))
	val compAll = conf.getBoolean("watchers.startup.full-compile")
	
	conf.getStringList("watchers.startup.directories").foreach { dir =>
		watchers += dir -> new FileSystemWatcher(dir, compiler, compAll)
	}

	/** Processes the arguments passed through the mini console mode.
	 */
	def processArgs(args: List[String]): Unit = args match {
		case Nil => 
		case "clear" :: rest =>
			watchers.values.foreach { _.purge }
			watchers.clear
			processArgs(rest)
		case "watch" :: Path(dir) :: rest =>
			if(watchers.get(dir).isDefined){
				println("Watch is already active")
			} else {
				val dirFile = new File(dir)
				if(dirFile.exists) {
					val path = dirFile.getAbsolutePath
					watchers += dir -> new FileSystemWatcher(path, compiler, false)
				} else {
					println("target does not exist.")
				}
					
			}
			processArgs(rest)
		case "remove" :: Path(dir) :: rest =>
			watchers.remove(dir).fold[Unit] {
				println(s"No watch for directory $dir found.")
			} { listen =>
				listen.purge
				println("Watch succesfully removed")
			}
			processArgs(rest)
		case "all" :: rest =>
			watchers.values.foreach { w => compiler.compileAll(w.dirName) }
			processArgs(rest)
		case "exit" :: rest =>
			watchers.values.foreach { _.purge }
			println("Exiting...")
			System.exit(0)
		case skip :: rest =>
			println("argument not processed: " + skip)
			processArgs(rest)
	}
	
	val argPat = """(["][\\A-z0-9\/ ]+["])|([ ]?[\\A-z0-9\/-]+[ ])|([\\A-z0-9\/-]+)""".r
	while(true){
		//print("\n> ")
		//flush()
		val input = readLine
		println("")
		val consArgs = argPat.findAllMatchIn(input).map { _.toString.trim }
		processArgs(consArgs.toList)
	}
}


