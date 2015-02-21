package aghost7.nwscriptconsole

import scala.annotation._
import java.io.File
import java.net.URL

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
	def unapply(line: String): Option[String] = {
		for(c <- line){
			// Enforce compilation to tableswitch/lookupswitch
			(c: @switch) match {
				// just keep going...
				case ' ' | '\t' =>
				// could be what we're looking for so we can't skip this line
				case 'v' | 'i' | '#' => return None 
				case _ => return Some(line)
			}
		}
		None
	}
}
object MainLine {
	private val pat = """^\s*void\s*main\s*\([A-z0-9\s=]*\)""".r
	def unapply(s: String) = pat.findFirstIn(s)
}
object CondLine {
	private val pat = """^\s*int\s*StartingConditional\s*\([A-z0-9\s=]*\)""".r
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
	def fromList(name: String, path: String, source: List[String]): NssFile = {
		@tailrec
		def process(lines: List[String], 
				isMain: Boolean = false,
				isCond: Boolean = false, 
				incs: List[String] = Nil):NssFile = lines match {
			case Nil =>
				new NssFile(name, path, isMain, isCond, incs)
			case SkippableLine(line) :: rest =>
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
	 *  @param fl is the target file.
	 */
	def fromFile(fl: File): NssFile = {
		val path = fl.getAbsolutePath()
		// Will be used later to resolve include changes.
		val name = fl.getName().replace(".nss", "").replace(".NSS", "")
		val lines = io.Source.fromFile(path).getLines.toList
		fromList(name, path, lines)
	}
	

	
	/** Constructs an NssFile instance.
	 *  n.b., This is blocking IO, and a bit intensive.
	 */
	def apply(target: String) = fromFile(new File(target))
	
	
	/** Unapply used for pattern matching in tail recursive functions.
	 *  
	 *  @return name, includes list and isInclude
	 */
	def unapply(nss: NssFile): Option[(String, List[String], Boolean)] = 
		Some((nss.name, nss.includes, nss.isInclude))
}