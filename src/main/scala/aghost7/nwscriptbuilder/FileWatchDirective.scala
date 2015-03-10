package aghost7.nwscriptbuilder


import java.nio.file._
import java.nio.file.StandardWatchEventKinds._
import java.lang.InterruptedException
import scala.collection.JavaConversions._
import akka.actor.{ActorRef,Actor, Props}
import scala.util.matching.Regex

/** Events which are generated by the FileWatchScheduler */
sealed trait FileEvent
case class FileCreated(path: String) extends FileEvent
case class FileRemoved(path: String) extends FileEvent
case class FileModified(path: String) extends FileEvent

/** Messages which the FileWatchScheduler can accept */
sealed trait WatchRequest
case class StartWatch(path: String) extends WatchRequest
case class StopWatch(path: String) extends WatchRequest
case object ClearWatch extends WatchRequest

/** Actor abstraction over the java nio api. */
class FileWatchScheduler(filter: Option[Regex]) extends Actor {
	
	implicit val tag = LoggerTag("FileWatchScheduler :: ")
	
	val watcher = new FileWatchProcessor(context.parent, filter)
	
	override def preStart() = watcher.start 
	override def postStop() = {
		Logger.debug("Stopping watcher")
		watcher.stop
	}
	
	def receive = {
		
		case StartWatch(path) =>
			Logger.debug("Starting watch for " + path)
			var ls =  watcher.listening :+ Paths.get(path)
			watcher.listening = ls
			watcher.reboot
			
		case StopWatch(path) =>
			Logger.debug("Stopping watch for " + path)
			val p = Paths.get(path)
			val ls = watcher.listening.filterNot { pt => p.equals(pt) }
			watcher.listening = ls
			watcher.reboot
		
		case ClearWatch =>
			Logger.debug("Clearing all watches")
			watcher.listening = Nil
			watcher.reboot
			
	}
	
}

object FileWatchScheduler {
	def props(filter: Option[Regex]) = Props(new FileWatchScheduler(filter))
}

class FileWatchProcessor(parent: ActorRef, filter: Option[Regex]) 
		extends Runnable {
	
	implicit val tag = LoggerTag("FileWatchProcessor :: ")
	
	val fs = FileSystems.getDefault()
	private val t = new Thread(this)

	var listening = Seq[Path]()
	
	@volatile private var active = true
	
	def tryLoop(ls: Seq[Path]) {
		val watch = fs.newWatchService()
		val watching = ls.map { (p) =>
			val key = p.register(watch, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
			(key, p)
		}
		try {
			while(active) {
				val key = watch.take
				for(ev <- key.pollEvents) {
					val rel = ev.asInstanceOf[WatchEvent[Path]].context
					val path = key
						.watchable()
						.asInstanceOf[Path]
						.resolve(rel)
						.toAbsolutePath()
						.toString
					
					if(filter.map { _.findFirstIn(path).isDefined }.getOrElse(true)){
						ev.kind match {
							case ENTRY_CREATE => parent ! FileCreated(path)
							case ENTRY_MODIFY => parent ! FileModified(path)
							case ENTRY_DELETE => parent ! FileRemoved(path)
							case _ => //meh
						}
					}
				}
				
				if(!key.reset){
					// TODO: put something...
				}
			}
		} catch {
			case _: InterruptedException =>
				watch.close()
				if(active) {
					Logger.debug("Reloading thread data.")
					tryLoop(listening)
				} else {
					Logger.debug("Thread shutting down.")
				}
			
		}
	}
	
	def run = tryLoop(listening)
	
	def start = t.start()
	
	/** Interrupting will force the thread to reload if still set as active. 
	 *  Otherwise it will continue to use the reference to the old list. 
	 */
	def reboot = t.interrupt()
	
	def stop = {
		active = false
		t.interrupt()
	}
}