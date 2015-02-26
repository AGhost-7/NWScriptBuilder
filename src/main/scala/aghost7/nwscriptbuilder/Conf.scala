package aghost7.nwscriptbuilder

import java.io.File

import com.typesafe.config._
import scala.collection.JavaConversions._

object Conf {
	
	
	private val userFile = {
		val jarPath = getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath
		val jar = new File(jarPath)
		val path = jar.getParentFile().getAbsolutePath() + "/application.conf"
		new File(path)
	}
	
	def userFileFound = userFile.exists
	val get = {
		
		val userConf = ConfigFactory.parseFile(userFile)
		
		ConfigFactory.load(userConf) 
	}
}