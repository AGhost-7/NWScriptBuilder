package aghost7.nwscriptconsole


import java.nio.file._
import java.nio.file.StandardWatchEventKinds._
import java.lang.InterruptedException
import util._


/** Run watch logic on separate thread to allow user to input other arguments 
 *  in the console.
 */
class FileSystemWatcher(
		dirName: String, 
		compiler: CompilerProcessor, 
		initCompileAll: Boolean) 
		extends Runnable 
		with IncludeTracker {
	
	private val fs = FileSystems.getDefault()
	private val watch = fs.newWatchService()
	private val dir = fs.getPath(dirName)
	private val key = dir.register(watch, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
	
	private val t = new Thread(this)
	
	t.start()
	
	def msg(s: String) = Console.println(s"\nWatcher ($dirName): $s")
	
	def tick = {
		Console.print("> ")
		Console.flush()
	}
	
	def processNssChange(kind: WatchEvent.Kind[_], dirPath: String) {
		loadDirectoryNssFiles(new java.io.File(dirName))
		//msg("file changed: " + dirPath)
		val (isInclude, absPath) = include(dirPath)
		if(isInclude){
			// then I need to check which other files that were affected...
			val recomp = dependees(absPath)
			msg("compiling depending files")
			recomp.foreach { file => 
				compiler.compile(file)
			}
		} else {
			msg("compiling file: " + dirPath)
			// we can just compile the one file.
			compiler.compile(absPath)
		}
		msg("waiting...")
		tick
	}
	
	def run: Unit = {
		try {
			if(initCompileAll){
				msg("compiling all.")
				compiler.compileAll(dirName)
				tick
			}
			msg("waiting...")
			tick
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
				msg("service closed.")
				tick
			case err: Throwable =>
				msg("unexpected exception")
				
				
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