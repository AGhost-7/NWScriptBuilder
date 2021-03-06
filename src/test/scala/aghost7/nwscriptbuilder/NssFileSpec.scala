package aghost7.nwscriptbuilder

import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class NssFileSpec extends FlatSpec with Matchers {
	
	implicit val tag = LoggerTag("")
	
	def fetchNss(target: String) = 
		NssFile("src/test/resources/" + target)
		
	val foobar = fetchNss("foobar.nss")
	behavior of "include with no dependencies"
	it should "have proper boolean flags" in {
		foobar.isInclude should be (true)
		foobar.isMain should be (false)
		foobar.isStartCond should be (false)
	}
	it should "have no include statements"  in {
		foobar.includes.isEmpty should be (true)
	}
	
	val foobar2 = fetchNss("foobar2.nss")
	"include with one dependency" should "have proper boolean flags" in {
		foobar2.isInclude should be (true)
		foobar2.isMain should be (false)
		foobar2.isStartCond should be (false)
	}
	it should "have one include statement" in {
		foobar2.includes.length should be (1)
	}
	
	val hello = fetchNss("hello.nss")
	"main with one include" should "have proper boolean flags" in {
		hello.isInclude should be (false)
		hello.isMain should be (true)
		hello.isStartCond should be (false)
	}
	it should "have one include statement" in {
		hello.includes.length should be (1)
	}
	
	"conversation scripts" should "ga scripts as mains" in {
		val source = """
			void main(string greet="person", int x = 1){
			""".lines
		source.exists{ 
			case MainLine(s) => true 
			case _ => false
		} should be (true)
	}
	it should "gc scripts as starting conditionals" in {
		val source = """
			int StartingConditional(int nLevelRequired=20, string foo_bar)
			""".lines
		source.exists {
			case CondLine(s) => true
			case _ => false
		} should be (true)
	}
}