package aghost7.nwscriptbuilder

import java.io.File

import sys.process._
import Console._
import scala.collection.JavaConversions._

import com.typesafe.config.Config

class CompilerProcessor(
		compilerLoc: String,
		baseArgs: String, 
		fullCompCol: List[String],
		multiSpawn: Boolean,
		filterOutput: Boolean) {
	
	private val compFile = new File(compilerLoc)
	private val compAbs = compFile.getAbsolutePath
	private val compDir = compFile.getParentFile()
	private val compName = compFile.getName
	
	/** Logging pipe.
	 */
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
					print("> ")
					flush
				} else if(!filterOutput) {
					Logger.info(line, false)
				}
			}
		}, { line => 
			Logger.error(line)
		})
	
	/** Returns the constructed command to use for the compiler
	 */
	def batchCommand(dirName: String, files: Seq[String]): String = 
		s""""$compAbs" -b "$dirName" $baseArgs ${files.mkString(" ")}"""
	
	def simpleCommand(file: String): String =
		s""""$compAbs" $baseArgs $file"""
	

	/** Computes how the load is going to be distributed. Checks files 
	 *  recursively.
	 *  
	 *  @param rootDir is the starting directory.
	 */
	def partitionParallel(rootDir: File): Seq[Seq[String]] = {
		
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
		fullCompCol.map{ chars => startingWith(chars, rootDir) }
	}
		
	/** Compiles the entire given directory.
	 *  
	 *  @param dirName is the absolute path of the target directory to compile.
	 */
	def compileAll(dirPath: String)(implicit tag: LoggerTag) {
		if(multiSpawn){
			val dir = new File(dirPath)
			val parts = partitionParallel(dir)
			for(charsParts <- parts){
				if(!charsParts.isEmpty){
					val cmd = batchCommand(dirPath, charsParts)
					Logger.debug("running command: " + cmd)
					cmd.run(loggingPipe)
				}
				
			}
		} else {
			val cmd = simpleCommand(dirPath + "/*.nss")
			Logger.debug("running command: \n" + cmd)
			cmd.run(loggingPipe)
		}
	}
	
	def compileList(dirName: String, fileNames: List[NssFile])(implicit tag: LoggerTag){
		val fls = fileNames
				.map { nss => "\"" + nss.path + "\"" }
				
		val cmd = batchCommand(dirName, fls)
		Logger.debug("Running command: " + cmd)
		cmd.run(loggingPipe)
	}
	
	/** Compiles the file at the given absolute path.
	 */
	def compile(absPath: String)(implicit tag: LoggerTag) {
		val cmd = simpleCommand('"' + absPath + '"')
		Logger.debug("running command: " + cmd)
		cmd.run(loggingPipe)
	}
	
	def compile(nss: NssFile)(implicit tag: LoggerTag): Unit = compile(nss.path)
	
}

object CompilerProcessor {
	
	/** Creates a new CompilerProcessor from the configuration object given.
	 */
	def fromConfig(conf: Config): CompilerProcessor = {
		val confInc = conf.getStringList("include-directories").toArray()
		val incCmd = 
			if(confInc.isEmpty) ""
			else " -i \"" + confInc.mkString(";") + "\""
		val compilerLoc = conf.getString("root")
		val baseArgs = conf.getString("options") + incCmd
		val multiSpawn = conf.getBoolean("multi-processes")
		val letters: List[String] = 
			if(multiSpawn) conf.getStringList("process-assignments").toList
			else Nil
		val filter = conf.getBoolean("filter-output")
		new CompilerProcessor(compilerLoc, baseArgs, letters, multiSpawn,filter)
	}
	
	
}