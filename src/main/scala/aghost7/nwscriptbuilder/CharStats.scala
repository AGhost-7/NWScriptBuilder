package aghost7.nwscriptbuilder

trait CharStats {
	
	import DirectoriesMapping._
	
	/** Takes the complete directories mapping and turns it into a simple file 
	 *  name list.
	 */
	def flattenToNames(m : DirectoriesMap): List[String] = 
		m.values.flatMap { m2 =>
			m2.values.map { nss => nss.name }
		}.toList
	
	
	/** Takes a list of file names and counts their first letters.
	 *  
	 *  @param directories is a list of file names. It musn't be a list of paths,
	 *  just the name (e.g., foobar.txt).
	 *  @return A list of chars with the number of times it is in the first 
	 *  letter
	 */
	def charCount(directories: DirectoriesMap): List[(Char, Int)] = 
		flattenToNames(directories).foldLeft(Map[Char, Int]()) { (total, name) =>
			val newCount = total.getOrElse(name(0), 0) + 1
			val charCount = name(0) -> newCount
			total + charCount
		}.toList
		
	/** This goes even further and recommends you a specific combination of 
	 *  characters to use for splitting across the specified number of processes.
	 */
	def recommendChars(directories: DirectoriesMap, processes: Int) : List[String] = {
		val stats = charCount(directories)
		// find the x highest in the collection
		val highest = stats
				.sortBy{ _._2 }
				.takeRight(processes)
				
		// Find what is left for us to process.
		val rest = stats.filterNot { 
			case (c1, cnt1) => highest.exists { case (c2, cnt2) => c1 == c2 } 
		}
				
		// now I need to convert it to my base for the fold.
		val sHighest = highest.map { case(c,n) => (c + "", n) }
		
		rest.foldLeft(sHighest) { case (result, (char, count)) => 
			// find the best candidate for adding.
			val (smChar, smCount) = result.foldLeft((" ", Int.MaxValue)) { 
				case(pick, iter) =>
					if(iter._2 < pick._2) iter
					else pick
			}
			
			val diff = result.filterNot { case (s, n) => s == smChar }
			
			val withChar = (smChar + char, smCount + count)
			
			withChar :: diff
		}.map { _._1 }
	}
	
	
	
}