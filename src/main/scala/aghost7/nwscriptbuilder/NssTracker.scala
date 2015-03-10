package aghost7.nwscriptbuilder

import akka.actor.{Actor,Props}
import scala.concurrent.duration.Duration
import scala.collection.mutable.{Map => MMap}
import java.util.concurrent.TimeUnit

sealed trait Cmd
case class WatchCmd(path: String, compAll: Boolean) extends Cmd
case object ProcessChangesCmd extends Cmd
case object CompAllCmd extends Cmd
case object CharsCountCmd extends Cmd
case class CharsRecommendCmd(ps: Int) extends Cmd
case class RemoveCmd(path: String) extends Cmd

class NssTracker(compiler: CompilerProcessor) 
		extends Actor 
		with NssReading 
		with DirectoriesMapping 
		with CharStats 
		with UXControls {
	
	import DirectoriesMapping._
	
	implicit val tag = LoggerTag("")
	
	/** To ensure that events aren't double, or even tripple-triggered by other
	 *  applications, the file system events are buffered.
	 */
	var check: List[String] = Nil
	
	val checkDelay = Duration(200, TimeUnit.MILLISECONDS)
	
	override def preStart() {
		val filter = Some("[.][nN][sS][sS]$".r)
		context.actorOf(FileWatchScheduler.props(filter), "scheduler")
	}
	
	def mkTag(path: String) : LoggerTag = {
		val watcherName = 
			if(path.length > 25) "..." + path.drop(path.length - 25)
			else path
		LoggerTag(watcherName + " :: ")
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
				} else {
					tick
				}
				
				val base = directoryNssFiles(new java.io.File(path))
				appendForDirectory(path, base)
				context.child("scheduler").get ! StartWatch(path) 
			}
			
		case ProcessChangesCmd =>
			Logger.debug("Update files:\n" + check.distinct.mkString("\n"))
			val nssGroups = updateAtPaths(check.distinct)
			// now that they're updated, compile em.
			for((nss, dirPath, nssDir) <- nssGroups){
				implicit val tag = mkTag(dirPath)
				println("")
				if(nss.isInclude){
					val depend = findDependees(nss, nssDir.values)
					compiler.compileList(dirPath, depend)
				} else {
					compiler.compile(nss.path)
				}
			}
			check = Nil
			
		case CompAllCmd =>
			directories.keys.foreach { dirPath =>
				compiler.compileAll(dirPath)
			}
			
		case CharsRecommendCmd(processes) =>
			val chars = recommendChars(directories, processes)
			val charCombos = chars
				.map { charset => "\"" + charset + "\"" }
				.mkString(", ")
			toClipboard("[" + charCombos + "]")
			Logger.info("Recommended combination: " + chars.mkString(", "))
			tick
			
		case CharsCountCmd =>
			val statsStr = charCount(directories)
				.sortBy {  _._2 }
				.map { case (char, count) => char + " = " + count }
				.mkString("\n")
				
			Logger.info("-Character stats for watched files-\n" + statsStr)
			tick
				
		case RemoveCmd(path) =>
			// TODO
			
		case FileCreated(path) =>
			if(check.isEmpty){
				val sys = context.system
				sys
					.scheduler
					.scheduleOnce(checkDelay, self, ProcessChangesCmd)(sys.dispatcher)
			}
			check = path :: check
			
		case FileModified(path) =>
			if(check.isEmpty){
				val sys = context.system
				sys
					.scheduler
					.scheduleOnce(checkDelay, self, ProcessChangesCmd)(sys.dispatcher)
			}
			check = path :: check

		case FileRemoved(path) =>
			val (dirPath, nssDir) = findDirectory(path)
			nssDir.remove(path).fold {
				Logger.error("Could not find deleted file in directory mappings.")
			} { file =>
				Logger.debug("File removed: " + path)
			}
			
		case ClearWatch =>
			context.child("scheduler").get ! ClearWatch

	}
}

object NssTracker {
	def props(c: CompilerProcessor) = Props(new NssTracker(c))
}