package aghost7.nwscriptconsole

import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import com.typesafe.config._

@RunWith(classOf[JUnitRunner])
class CompilerProcessorSpec extends FlatSpec with Matchers {
	val conf = ConfigFactory.load()
	val compiler = CompilerProcessor.fromConfig(conf.getConfig("compiler"))
	
	
	
}