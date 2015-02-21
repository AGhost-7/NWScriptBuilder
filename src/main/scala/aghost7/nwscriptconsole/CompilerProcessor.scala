package aghost7.nwscriptconsole

import com.typesafe.config._
import sys.process._
import Console._

class CompilerProcessor(val baseCmd: String) {
	private val compilerIn = 
		ProcessLogger({ line => println(line)}, { line => println(line)})
	
	/** Compiles the entire given directory.
	 *  
	 *  -TODO- parallel compile this shit.
	 */
	def compileAll(dirName: String) {
		Process(s"""$baseCmd "$dirName/*.nss"""") !< compilerIn
	}
	
	/** Compiles the file at the given absolute path.
	 */
	def compile(absPath: String): Unit = {
		Process(baseCmd + " " + absPath) !< compilerIn
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
		val compilerCmd = conf.getString("root") + " " + 
			conf.getString("options") + incCmd
		new CompilerProcessor(compilerCmd)
	}
	
	
}