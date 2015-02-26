package aghost7.nwscriptbuilder

import java.io.File

import java.nio.file._
import java.nio.file.StandardWatchEventKinds._
import java.lang.InterruptedException

import scala.collection.JavaConversions._

/** Run watch logic on separate thread to allow user to input other arguments 
 *  in the console.
 */
class FileSystemWatcher(
		val dirName: String, 
		compiler: CompilerProcessor, 
		initCompileAll: Boolean) 
		extends Runnable 
		with IncludeTracker {

	private val fs = FileSystems.getDefault()
	private val watch = fs.newWatchService()
	private val dir = fs.getPath(dirName)
	private val key = dir.register(watch, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
	val watcherName = 
		if(dirName.length > 25) "..." + dirName.drop(dirName.length - 25)
		else dirName
	implicit val tag = LoggerTag(s"Watcher ($watcherName): ")
	private val t = new Thread(this)
	
	t.start()
	
	/** Sends a message to the user with the watcher's name
	 */
	//def msg(s: String) = Console.println(s"\nWatcher ($dirName): $s")
	
	/** Leave a tick to "hint" user that they can type into the console.
	 */
	def tick = {
		Console.print("> ")
		Console.flush()
	}
	
	/** Called if file is an nss file to process changes.
	 */
	def processNssChange(kind: WatchEvent.Kind[_], dirPath: String) {
		if(kind == ENTRY_DELETE){
			remove(dirPath)
		} else {
			val (isInclude, absPath) = include(dirPath)
			Logger.debug("File is include: " + isInclude)
			if(isInclude){
				// then I need to check which other files that were affected...
				val recomp: List[NssFile] = dependees(absPath)
				println("")
				compiler.compileList(dirName, recomp)
			} else {
				println("")
				// we can just compile the one file.
				compiler.compile(absPath)
			}
		}
	}
	
	/** Watcher thread loop
	 */
	def run: Unit = {
		try {
			if(initCompileAll){
				Logger.info("compiling all.")
				println("")
				compiler.compileAll(dirName)
			}
			loadDirectoryNssFiles(new File(dirName))
			Logger.info("waiting...")
			while(!t.isInterrupted()) {
					val evs = watch.take.pollEvents
					for(ev <- evs) {
						val kind = ev.kind
						if(kind == OVERFLOW){
							// error?
						} else {
							// ugh, this java api is terrible.
							val pathEv = ev.asInstanceOf[WatchEvent[Path]]
							val file = pathEv.context
							val full = dir.resolve(file).toString
							if(full.toLowerCase.endsWith(".nss")){
								Logger.debug(s"event to nss file $full detected.")
								processNssChange(kind, full)
							}
							if(!key.reset){
								t.interrupt()
							}
						}
					}
				
			}
		} catch {
			case _: ClosedWatchServiceException | _: InterruptedException =>
				Logger.info("service closed.")
				tick
			case err: Throwable =>
				Logger.error("unexpected exception")
				err.printStackTrace()
				tick
		}
	}
	
	/** Ends the file system watch
	 */
	def purge: Unit = {
		watch.close()
		t.interrupt()
	}
}