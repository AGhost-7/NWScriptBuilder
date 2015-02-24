package aghost7.nwscriptbuilder

import com.typesafe.config._
import sys.process._
import Console._
import scala.collection.JavaConversions._
import java.io.File




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
	
	/** Create compiler output pipe based on settings.
	 */
	private val compilerIn = 
		if(filterOutput)
			ProcessLogger({ line => 
				if(line.startsWith("Compiling") 
						|| line.startsWith("Error")){
					println("")
					println(line) 
				} else if(line.startsWith("Total Execution")) {
					println(line)
					print("> ")
					flush
				}
			}, { line => 
				println(line)
			})
		else
			ProcessLogger({ line => 
				if(line.startsWith("Total Execution")) {
					println(line)
					print("> ")
					flush
				} else if(!line.isEmpty){
					println(line)
				}
			}, { line => 
				println(line)
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
	def compileAll(dirPath: String) {
		if(multiSpawn){
			val dir = new File(dirPath)
			val parts = partitionParallel(dir)
			for(charsParts <- parts){
				if(!charsParts.isEmpty){
					val cmd = batchCommand(dirPath, charsParts)
					cmd.run(compilerIn)
				}
				
			}
		} else {
			Process(simpleCommand(dirPath + "/*.nss"), compDir).run(compilerIn)
		}
	}
	
	def compileList(dirName: String, fileNames: List[NssFile]){
		val fls = fileNames
				.map { nss => "\"" + nss.path + "\"" }
				
		val cmd = batchCommand(dirName, fls)
		cmd.run(compilerIn)
	}
	
	/** Compiles the file at the given absolute path.
	 */
	def compile(absPath: String): Unit = {
		val cmd = simpleCommand(absPath)
		cmd.run(compilerIn)
	}
	
	def compile(nss: NssFile): Unit = compile(nss.path)
	
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