package aghost7.nwscriptbuilder

import scala.collection.mutable.{Map => MMap}

object DirectoriesMapping {
	
	/** A directory being watched is represented as a map of nss file instances,
	 *  where the key is the absolute path 
	 */
	type NssDir = MMap[String, NssFile]
	
	/** Key is the directorie's absolute path. */
	type DirectoriesMap = MMap[String, NssDir]
}


trait DirectoriesMapping {
	
	private implicit val tag = LoggerTag("DirectoriesMapping :: ")
	
	import DirectoriesMapping._
	
	
	/** This is the collection of directories being watched by the app. The key 
	 *  here is the absolute path of the directory
	 */
	val directories: DirectoriesMap = MMap[String, NssDir]()
	
	/** Searches for the directory containing the given absolute file path 
	 *  Returns a tuple of the absolute path of the directory and the mapping
	 *  of that directory,
	 */
	def findDirectory(file: String): (String, NssDir) = {
		Logger.debug("Searching for directory of : " + file)
		val dirs = directories
				.filter { case (path, nssFiles) => file.startsWith(path) }
		
		if(dirs.isEmpty) 
			throw new NoSuchElementException("Could not find directory.")
		
		// find the most specific directory name, ie the longest match for our file.
		val longest = dirs.foldLeft(0) { case (l, (dir, nssFiles)) => 
				if(l < dir.length) dir.length else l
			}
		
		val deepest = dirs.filter { case(dir, nssFiles) => dir.length == longest }
			
		if(deepest.size > 1)
			throw new RuntimeException("Cannot resolve change to a single directory.")
		
		val key = deepest.keys.toSeq(0)
		(key, deepest(key))
	}
	
	def updateAtPaths(files: List[String]): List[(NssFile, String, NssDir)] = 
		for(upFile <- files) yield {
			// first resolve the parent directory of the changed file
			val (dirPath, nssDir) = findDirectory(upFile)
			// now I need to update each one
			val nss = NssFile(upFile)
			nssDir += nss.path -> nss
			// And return the nss file to update. State on the maps is dirty and I
			// need to update everything before I can walk through the dependencies.
			(nss, dirPath, nssDir)
		}
	
	/** Appends collection to corresponding directory. Creates a new map if the
	 *  directory doesn't exist yet.
	 */
	def appendForDirectory(dir: String, files: Iterable[NssFile]) {
		directories.get(dir).fold[Unit] {
			val base = files.foldLeft(MMap[String, NssFile]()) { (mp, nss) =>
				mp += nss.path -> nss
			}
			directories += dir -> base
		} { dirMap => 
			files.foreach { nss => dirMap += nss.path -> nss }
		}
	}
	
}