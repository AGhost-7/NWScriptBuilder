package aghost7.nwscriptbuilder

import scala.annotation._
import java.io.File

/** Entirely immutable representation of what we need to know about the nss 
 *  file. Updates to the file will replace the instance, instead of modifying 
 *  the existing instance.
 */
class NssFile (
		val name: String, 
		val path: String, 
		val isMain: Boolean,
		val isStartCond: Boolean,
		val includes: List[String]) {
	val isInclude = !isMain && !isStartCond
	override def toString = 
		s"""name: $name 
		|path: $path
		|isMain: $isMain, 
		|isStartCond: $isStartCond, 
		|isInclude: $isInclude
		|includes: $includes""".stripMargin
}

/** This is to avoid using regular expressions for every line in the file. 
 *  Application would have some pretty poor performance otherwise. 
 */
object SkippableLine {
	def apply(line: String): Boolean = {
		for(c <- line){
			// Enforce compilation to tableswitch/lookupswitch
			(c: @switch) match {
				// just keep going...
				case ' ' | '\t' =>
				// could be what we're looking for so we can't skip this line
				case 'v' | 'i' | '#' => return false
				case _ => return true
			}
		}
		false
	}
}
object MainLine {
	private val pat = """^\s*void\s*main\s*\([A-z0-9\s=,_"]*\)""".r
	def unapply(s: String) = pat.findFirstIn(s)
}
object CondLine {
	private val pat = """^\s*int\s*StartingConditional\s*\([A-z0-9\s=,_"]*\)""".r
	def unapply(s: String) = pat.findFirstIn(s)
}

object NssFile {
	private val includePattern = """^(\s*#include\s+["])([A-z0-9_]+)""".r
	
	/** Constructs a NssFile instance from provided source code.
	 *
	 *  @param name is the file's name without the extension
	 *  @param path is the absolute path of the file which the source code
	 *  comes from.
	 *  @param source is the contents of the file, line by line.
	 */
	def fromList(name: String, 
			path: String, 
			source: List[String])
			(implicit tag: LoggerTag): NssFile = {
		@tailrec
		def process(lines: List[String], 
				isMain: Boolean = false,
				isCond: Boolean = false, 
				incs: List[String] = Nil):NssFile = lines match {
			case Nil =>
				new NssFile(name, path, isMain, isCond, incs)
			case line :: rest if(SkippableLine(line)) =>
				process(rest, isMain, isCond, incs)
			case MainLine(matches) :: rest =>
				process(rest, true, isCond, incs)
			case CondLine(matches) :: rest =>
				process(rest, isCond, true, incs)
			case head :: rest =>
				val matches = includePattern.findFirstMatchIn(head)
				val newIncs = matches.fold { incs } { _.group(2) :: incs }
				process(rest, isMain, isCond, newIncs)
			
		}
		process(source)
	}
	
	/** Uses the scala.io.Source to create a NssFile instance
	 *  
	 *  @param file is the target file.
	 */
	def fromFile(file: File)(implicit tag: LoggerTag): NssFile = {
		val path = file.getAbsolutePath()
		Logger.debug(s"Opening file: $path")
		// Will be used later to resolve include changes.
		val name = file.getName().replace(".nss", "").replace(".NSS", "")
		val src = io.Source.fromFile(path)
		val lines = src.getLines.toList
		src.close// no lending pattern? :<
		fromList(name, path, lines)
	}
	

	
	/** Constructs an NssFile instance.
	 *  n.b., This is blocking IO, and a bit intensive.
	 */
	def apply(target: String)(implicit tag: LoggerTag) = fromFile(new File(target))
	
	
	/** Unapply used for pattern matching in tail recursive functions.
	 *  
	 *  @return name, includes list and isInclude
	 */
	def unapply(nss: NssFile): Option[(String, List[String], Boolean)] = 
		Some((nss.name, nss.includes, nss.isInclude))
}