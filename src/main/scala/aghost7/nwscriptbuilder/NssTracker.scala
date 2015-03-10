package aghost7.nwscriptbuilder

import akka.actor.{Actor,Props}
import scala.concurrent.duration.Duration
import scala.collection.mutable.{Map => MMap}
import java.util.concurrent.TimeUnit

sealed trait Cmd
case class WatchCmd(path: String, compAll: Boolean) extends Cmd
case object ProcessChangesCmd extends Cmd



class NssTracker(compiler: CompilerProcessor) extends Actor with NssReading {
	
	implicit val tag = LoggerTag("NssTracker :: ")
	
	/** To ensure that events aren't double, or even tripple triggered by other
	 *  applications, the file system events are buffered.
	 */
	var check: List[String] = Nil
	
	/** A directory being watched is represented as a map of nss file instances,
	 *  where the key is the absolute path 
	 */
	type NssDir = MMap[String, NssFile]
	
	/** This is the collection of directories being watched by the app. The key 
	 *  here is the absolute path of the directory
	 */
	val directories = MMap[String, NssDir]()
	
	override def preStart() {
		val filter = Some("[.][nN][sS][sS]$".r)
		context.actorOf(FileWatchScheduler.props(filter), "scheduler")
	}
	
	def mkTag(path: String) : LoggerTag = {
		val watcherName = 
		if(path.length > 25) "..." + path.drop(path.length - 25)
		else path
		LoggerTag(s"Watcher ($watcherName): ")
	}
	
	def findDirectory(file: String): (String, NssDir) = {
		val dirs = directories
				.filter { case (path, nssFiles) => 
					file.startsWith(path) && 
						nssFiles.exists{ case(abs, nss) => abs == file}
				}
		
		if(dirs.isEmpty) 
			throw new NoSuchElementException("Could not find directory.")
		else if(dirs.size > 1)
			throw new RuntimeException("Cannot resolve change to a single file.")
		
		val key = dirs.keys.toSeq(0)
		(key, dirs(key))
	}
	
	def receive = {
		case WatchCmd(path, compAll) =>
			implicit val tag = mkTag(path)
			if(directories.contains(path)){
				Logger.error("Directory is already being watched")
			} else {
				if(compAll) {
					println("")
					Logger.info("Compiling all.")
					compiler.compileAll(path)
				}
				
				context.child("scheduler").get ! StartWatch(path) 
			}
			
		
		case ProcessChangesCmd =>
			Logger.debug("Update files:\n" + check.distinct.mkString("\n"))
			
			val nssGroups = for(upFile <- check.distinct) yield {
				// first resolve the parent directory of the changed file
				val (dirPath, nssDir) = findDirectory(upFile)
				// now I need to update each one
				val nss = NssFile(upFile)
				nssDir += nss.path -> nss
				// And return the nss file to update. State on the maps is dirty and I
				// need to update everything before I can walk through the dependencies.
				(nss, dirPath, nssDir)
			}
			
			for((nss, dirPath, nssDir) <- nssGroups){
				if(nss.isInclude){
					val depend = dependees(nss, nssDir.values)
					compiler.compileList(dirPath, depend)
				} else {
					compiler.compile(nss.path)
				}
			}
			
			check = Nil
			
			
		case FileCreated(path) =>
			if(check.isEmpty){
				val time = Duration(200, TimeUnit.MILLISECONDS)
				val sys = context.system
				sys.scheduler.scheduleOnce(time, self, ProcessChangesCmd)(sys.dispatcher)
			}
			check = path :: check
			
		case FileModified(path) =>
			if(check.isEmpty){
				val time = Duration(200, TimeUnit.MILLISECONDS)
				val sys = context.system
				sys.scheduler.scheduleOnce(time, self, ProcessChangesCmd)(sys.dispatcher)
			}
			check = path :: check

		case FileRemoved(path) =>
			Logger.debug("File removed: " + path)
			
		case ClearWatch =>
			context.child("scheduler").get ! ClearWatch
			
		case _ =>
	}
}

object NssTracker {
	def props(c: CompilerProcessor) = Props(new NssTracker(c))
}