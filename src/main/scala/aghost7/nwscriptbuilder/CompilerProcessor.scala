package aghost7.nwscriptbuilder

import com.typesafe.config._
import sys.process._
import Console._
import scala.collection.JavaConversions._
import java.io.File




class CompilerProcessor(
		val compilerLoc: String,
		val baseArgs: String, 
		val fullCompCol: List[String],
		val multiSpawn: Boolean) {
	
	private val compFile = new File(compilerLoc)
	private val compDir = compFile.getParentFile()
	private val compName = compFile.getName
	
	private val compilerIn = 
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
	
	/** Returns the constructed command to use for the compiler
	 */
	def batchCommand(dirName: String, files: Seq[String]): String = 
		s"""$compName -b "$dirName" $baseArgs ${files.mkString(" ")}"""
	
	def simpleCommand(file: String): String =
		s"""$compName $baseArgs $file"""
	

	/** Computes how the load is going to be distributed.
	 */
	def partitionParallel(rootDir: File) {
		
		def startingWith(chars: String, dir: File) = {
			val children = dir.listFiles
			val (dirs, files) = children.partition { _.isDirectory }
			val matches = files.flatMap { file =>
				val fileName = file.getName
				val cOpt = chars.find { c => fileName.startsWith(c+"") }
				// if the char was found, then I need to turn it into a wildcard expression
				cOpt.map { c =>
					val dir = file.getParentFile
					dir.getAbsolutePath + "/" + c + "*.nss"
				}
			}.distinct
			
		}
		/*
		val children = file.listFiles
		val (dirs, files) = children.partition{ _.isDirectory }
		
		val wildcards = (for {
			chars <- fullCompCol
			child <- files
			char <- chars
			if(child.getName.startsWith(char + "") )
		} yield {
			child.getParentFile().getAbsolutePath() + "/" + char + "*.nss"
		}).distinct
		*/
		
	}
		
	/** Compiles the entire given directory.
	 */
	def compileAll(dirName: String) {
		if(multiSpawn){
			for(chars <- fullCompCol){
				val fls = chars
					.map { c => "\"" + dirName + "/" + c + "*.nss\""  }
					
				val cmd = batchCommand(dirName, fls)
				Process(cmd, compDir).run(compilerIn)
			}
		} else {
			Process(simpleCommand(dirName + "/*.nss"), compDir).run(compilerIn)
		}
	}
	
	def compileList(dirName: String, fileNames: List[NssFile]){
		val fls = fileNames
				.map { nss => "\"" + nss.path + "\"" }
				
		val cmd = batchCommand(dirName, fls)
		Process(cmd, compDir).run(compilerIn)	
	}
	
	/** Compiles the file at the given absolute path.
	 */
	def compile(absPath: String): Unit = {
		val cmd = simpleCommand(absPath)
		Process(cmd, compDir).run(compilerIn)
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
		
		new CompilerProcessor(compilerLoc, baseArgs, letters, multiSpawn)
	}
	
	
}