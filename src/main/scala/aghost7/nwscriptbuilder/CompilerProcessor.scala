package aghost7.nwscriptbuilder

import java.io.File

import sys.process._
import scala.collection.JavaConversions._

import com.typesafe.config.Config



trait FilePartitioner {

	implicit val tag: LoggerTag

	val parChars: Seq[String]

	/** A compile order is a list of files that the compiler process will need to
		* compile.
		*/
	type CompileOrder = Seq[String]

	/** Computes how the load is going to be distributed. Checks files
		*  recursively.
		*
		*  @param rootDir is the starting directory.
		*/
	def partitionParallelDir(rootDir: File): Seq[CompileOrder] = {

		Logger.debug("Partitioning workload for directory: " + rootDir.getName)

		/** This will construct the exact list of wildcarded paths where there
			*  is a file which exists to be compiled.
			*/
		def startingWith(chars: String, dir: File): Seq[String] = {
			val children = dir.listFiles
			val (dirs, files) = children.partition { _.isDirectory }
			val absPath = dir.getAbsolutePath
			val fileNames = files.map { _.getName }

			val matches = chars.flatMap { char =>
				val c = "" + char
				if(fileNames.exists { _.startsWith(c) })
					Some("\"" + absPath + "/" + char + "*.nss\"")
				else
					None
			}

			// then I need to descend into the other directories.
			matches ++ dirs.flatMap { d => startingWith(chars, d) }
		}
		parChars.map{ chars => startingWith(chars, rootDir) }
	}

	/** Partition a list of paths into partitions based on the configuration of
		* character assignment.
		*/
	def partitionParallelList(paths: Seq[String]): Seq[CompileOrder] = {
		parChars.map { chars =>
			paths.filter { path =>
				val c = path(0)
				chars.exists { _ == c }
			}
		}
	}
}

class CompilerProcessor(
		compilerLoc: String,
		baseArgs: String,
		includes: String,
		// How the load is seperated to compile in parallel
		val parChars: List[String],
		multiSpawn: Boolean,
		filterOutput: Boolean,
		installDir: Option[String]
		) extends UXControls with FilePartitioner {

	implicit val tag = LoggerTag("CompilerProcessor :: ")
	
	private val compFile = new File(compilerLoc)
	private val compAbs = compFile.getAbsolutePath
	private val compDir = compFile.getParentFile()
	private val compName = compFile.getName

	/** Logging pipe. */
	private def loggingPipe(implicit tag: LoggerTag) =
		ProcessLogger({ line => 
			if(!line.isEmpty) {
				if(line.contains("Error:")
						|| line.contains("error(s);")){
					Logger.error(line, false)
				} else if(line.startsWith("Compiling")){
					Logger.info(line, false)
				} else if(line.startsWith("Total Execution")) {
					Logger.info(line, false)
					tick
				} else if(!filterOutput) {
					Logger.info(line, false)
				}
			}
		}, { line => 
			Logger.error(line)
		})

	/** Returns the constructed command to use for the compiler. */
	def batchCommand(dirName: String, files: Seq[String]): Seq[String] = {
		val withInstall = installDir.fold { Seq.empty[String] } { dir => Seq("-n", dir) }
		if(Logger.isDebug)
			Logger.debug("Batch for files: " + files.mkString(" "))
		val inc = if(includes == "") Seq.empty else Seq("-i", includes)

		Seq(compAbs, baseArgs) ++ inc ++ Seq("-b", dirName) ++ withInstall ++ Seq("-y") ++ files

	}

	def simpleCommand(file: String): Seq[String] = {
		if(Logger.isDebug)
			Logger.debug("Single compile command for: " + file)

		val inc = if(includes == "") Seq.empty else Seq("-i", includes)

		val withInstall = installDir.fold { Seq.empty[String] } { dir => Seq("\n", dir) }

		Seq(compAbs, baseArgs) ++ inc ++ withInstall ++ Seq(file)
	}

	def runBatchCommand(dirName: String, files: Seq[String])
			(implicit tag: LoggerTag): Unit = {
		val cmd = batchCommand(dirName, files)
		if(Logger.isDebug) Logger.debug("running command: " + cmd)
		cmd.run(loggingPipe)
	}
		
	/** Compiles the entire given directory. Always parallel compiles if the
	 *  feature is turned on by the user.
	 *  
	 *  @param dirPath is the absolute path of the target directory to compile.
	 */
	def compileAll(dirPath: String)(implicit tag: LoggerTag) {
		if(multiSpawn) {
			val dir = new File(dirPath)
			for {
				charsParts <- partitionParallelDir(dir)
				if(!charsParts.isEmpty)
			} runBatchCommand(dirPath, charsParts)
		} else {
			val cmd = simpleCommand(dirPath + "/*.NSS")
			if(Logger.isDebug) Logger.debug("running command: " + cmd)
			cmd.run(loggingPipe)
		}
	}

	/** Compile a list. Will run in parallel if the workload is above 20 files. */
	def compileList(dirName: String, fileNames: List[NssFile])
			(implicit tag: LoggerTag): Unit = {
		val fls = fileNames.map { _.path }
		if(fileNames.length > 20) {
			for {
				order <- partitionParallelList(fls)
				if(!order.isEmpty)
			} runBatchCommand(dirName, order)
		} else {
			runBatchCommand(dirName, fls)
		}
	}
	
	/** Compiles the file at the given absolute path. */
	def compile(absPath: String)(implicit tag: LoggerTag) {
		val cmd = simpleCommand('"' + absPath + '"')
		if(Logger.isDebug) Logger.debug("running command: " + cmd)
		cmd.run(loggingPipe)
	}
	
	def compile(nss: NssFile)(implicit tag: LoggerTag): Unit = compile(nss.path)
	
}

object CompilerProcessor {
	
	/** Creates a new CompilerProcessor from the configuration object given. */
	def fromConfig(conf: Config): CompilerProcessor = {
		val includes = conf.getStringList("include-directories").mkString(";")
		val compilerLoc = conf.getString("root")
		val baseArgs = conf.getString("options")
		val multiSpawn = conf.getBoolean("multi-processes")
		val letters: List[String] = 
			if(multiSpawn) conf.getStringList("process-assignments").toList
			else Nil
		val filter = conf.getBoolean("filter-output")


		val installDir =
			try Some(conf.getString("install-directory"))
			catch { case _: Throwable => None }
		new CompilerProcessor(compilerLoc, baseArgs, includes, letters, multiSpawn, filter, installDir)
	}
	
	
}