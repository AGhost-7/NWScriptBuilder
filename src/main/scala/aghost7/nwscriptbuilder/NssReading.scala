package aghost7.nwscriptbuilder


trait NssReading {

	/** Returns which files depend on the file given.
	 *  
	 *  @param file Must be the absolute path.
	 *  @return Will give the main and starting conditional files which actually
	 *  require re-compilation.
	 */
	def dependees(file: NssFile, files: Iterable[NssFile])
			(implicit tag: LoggerTag) : List[NssFile] = {
		def walkUp(nss: NssFile): Iterable[NssFile] = nss match {
			case NssFile(name, includes, true) =>
				val (incs, execs) = files
					.filter { _.includes.contains(name) }
					.partition { _.isInclude }
				val incsDeep = incs.flatMap { inc => walkUp(inc) }
				execs ++: incsDeep
			case NssFile(name, path, false) =>
				List(nss)
		}
		walkUp(file).toList
	}
	
	/** Recursively reads all nss files in the directory.
	 *  
	 *  @param dir is the target directory.
	 */
	def directoryNssFiles(dir: java.io.File)
			(implicit tag: LoggerTag): Seq[(String, NssFile)] = {
		val (dirs, nondirs) = dir.listFiles.partition { _.isDirectory }
		val nssFiles = nondirs
			.filter{ _.getName.toLowerCase.endsWith(".nss") }
			.map { fl => 
				val nss = NssFile.fromFile(fl)
				nss.path -> nss
			}
		dirs.map(directoryNssFiles).flatten.toSeq ++ nssFiles
	}
	
	/** Counts the starting letters of each script.
	 */
	def charStats(files: Iterable[NssFile])
			(implicit tag: LoggerTag): Map[Char, Int] = {
		val fileNames = files.map { _.name }
		fileNames.foldLeft(Map[Char, Int]()) { (total, name) =>
			val charCount = name(0) -> total.getOrElse(name(0), 0)
			total + charCount
		}
	}
}