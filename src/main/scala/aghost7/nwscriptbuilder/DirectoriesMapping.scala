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
		else if(dirs.size > 1)
			throw new RuntimeException("Cannot resolve change to a single file.")
		
		val key = dirs.keys.toSeq(0)
		(key, dirs(key))
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
	
	
}