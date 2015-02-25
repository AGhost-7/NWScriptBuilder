package aghost7.nwscriptbuilder

import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith


@RunWith(classOf[JUnitRunner])
class IncludeTrackerSpec extends FlatSpec with Matchers {
	implicit val tag = LoggerTag("")
	
	def fetchNss(target: String) = 
		NssFile("src/test/resources/" + target)
		
	val tracker = new IncludeTracker{
		implicit val tag = LoggerTag("")
	}
	
	val files = List("foobar.nss", "foobar2.nss", "hello.nss", "hello2.nss", 
			"hello3.nss", "hello4.nss")
	val paths = files.map { file =>
		val longPath = "src/test/resources/" + file
		file -> tracker.include(longPath)._2
	}.toMap
	
	
	"leaf include" should "be depended upon" in {
		val dependers = tracker.dependees(paths("foobar.nss"))
		dependers.length should be (3)
		val names = dependers.map { _.name }
		// only files which are compileable should be returned. Includes cannot
		// be directly compiled.
		names should not contain ("foobar2")
		names should contain ("hello")
		names should contain ("hello2")
		names should contain ("hello3")
	}
	
	"branch include" should "be depended upon" in {
		val dependers = tracker.dependees(paths("foobar2.nss"))
		dependers.length should be (1)
		val names = dependers.map { _.name }
		names should contain ("hello3")
	}
	
	"main files" should "have no dependees" in {
		val dependers = tracker.dependees(paths("hello.nss"))
		dependers.length should be (1)
	}
}