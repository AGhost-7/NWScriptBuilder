package aghost7.nwscriptconsole

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
			//if(line.startsWith("compiling") || line.startsWith("error")) 
				println(line) 
		}, { line => 
			println(line)
		})
	
		
	
	/** Compiles the entire given directory.
	 *  
	 *  -TODO- parallel compile this shit.
	 */
	def compileAll(dirName: String) {
		/*if(multiSpawn){
			for(chars <- fullCompCol){
				val cmds = chars
					.map { c => s"""$compName $baseArgs "$dirName/$c*.nss""""}
					//.mkString(" && ")
				println("-------------")
				println("executing command:")
				println("-------------")
				println(cmds)
				Process(cmds, compDir).run(compilerIn)
				Process(s"""$compName $baseArgs "$dirName/${chars(0)}*.nss"""").run
			}
		} else {*/
			Process(s"""$compilerLoc $baseArgs "$dirName/*.nss"""") !< compilerIn
		//}
			
		
	}
	
	/** Compiles the file at the given absolute path.
	 */
	def compile(absPath: String): Unit = {
		Process(s"""$compilerLoc $baseArgs "$absPath""""") !< compilerIn
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