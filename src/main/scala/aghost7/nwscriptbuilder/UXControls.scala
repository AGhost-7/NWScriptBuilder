package aghost7.nwscriptbuilder

/** Trait contains utilities for making the application more interactive. */
trait UXControls {
	
	def toClipboard(s: String) {
		import java.awt.Toolkit
		import java.awt.datatransfer.StringSelection
		val select = new StringSelection(s)
		Toolkit
			.getDefaultToolkit
			.getSystemClipboard
			.setContents(select, select)
	}
	
	def tick = {
		Console.print("> ")
		Console.flush
	}
}