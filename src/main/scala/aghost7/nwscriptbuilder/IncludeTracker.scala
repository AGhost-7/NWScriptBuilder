package aghost7.nwscriptbuilder

import scala.collection.mutable.{Map => MMap}

import scala.annotation._
import java.io.File


trait IncludeTracker {
	import java.nio.file.Path
	
	/** File Path -> List[Include Dependencies] */
	private val files = MMap[String, NssFile]()
	
	
	private val includePattern = """^(\s*#include\s+["])([A-z0-9_]+)""".r
	
	/** Evaluates what includes the file has.
	 *  
	 *  @param file Can be relative or absolute path.
	 *  @return true if the file is an include, along with the absolute path.
	 */
	def include(file: String): (Boolean, String) = {
		val nss = NssFile(file)
		files += nss.path -> nss
		(nss.isInclude, nss.path)
	}
	
	def include(file: File): Unit = {
		val nss = NssFile.fromFile(file)
		files += nss.path -> nss
	}
	
	/** Returns which files depend on the file given.
	 *  
	 *  @param file Must be the absolute path.
	 *  @return Will give the main and starting conditional files which actually
	 *  require re-compilation.
	 */
	def dependees(file: String): List[NssFile] = {
		val nss = files.get(file).get
		val fl = files.values
		
		// Walks up the dependency tree to find the main and starting conditional 
		// files.
		def walkUp(nss: NssFile): Iterable[NssFile] = nss match {
			case NssFile(name, includes, true) =>
				val (incs, execs) = fl
					.filter { _.includes.contains(name) }
					.partition { _.isInclude }
				val incsDeep = incs.flatMap { inc => walkUp(inc) }
				execs ++: incsDeep
			case NssFile(name, path, false) =>
				List(nss)
		}
		
		 walkUp(nss).toList
		
	}
	
	/** Recursively reads all nss files in the directory.
	 *  
	 *  @param dir is the target directory.
	 */
	def loadDirectoryNssFiles(dir: java.io.File): Unit = {
		val (dirs, nondirs) = dir.listFiles.partition { _.isDirectory }
		nondirs
			.filter{ _.getName.toLowerCase.endsWith(".nss") }
			.foreach { fl => 
				val nss = NssFile.fromFile(fl)
				files += nss.path -> nss
			}
		dirs.foreach(loadDirectoryNssFiles)
	}
	
	def remove(target: String){
		files.remove(target).get
	}
	
}




