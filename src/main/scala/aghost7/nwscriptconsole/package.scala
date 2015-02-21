package aghost7.nwscriptconsole


package object util {
	
	/** This new Java api breaks Scala interop...
	 */
	implicit class javaIteratorAddons[A](iter: java.lang.Iterable[A]){
		def foreach(func: A => Unit) {
			val it = iter.iterator
			while(it.hasNext){
				func(it.next())
			}
		}
	}
}