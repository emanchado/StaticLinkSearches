import scalaj.http.{Http, Token, HttpException}
import org.demiurgo.operalink.{LinkAPI, TestLinkServerProxy}
import scala.util.parsing.json.{JSON, JSONArray, JSONObject}

import org.fusesource.scalate._

package org.demiurgo {
  object StaticLinkSearches {
    def main(args: Array[String]) {
      val consumer = Token("8NrRjW2WhWjQIZNbVXZMCWveSkmvQJHn",
                           "BPwJIdhfOvUc91NRe634nyFSPPHYTDrx")
      var accessToken : Token = null
      val accessTokenFilePath = System.getenv("HOME") + "/" +
                                  ".staticlinksearches.json"
      var configJsonText: String = ""
      try {
        configJsonText = io.Source.fromFile(accessTokenFilePath).mkString
      } catch {
        case e: java.io.FileNotFoundException => configJsonText = "{}"
      }
      val config = JSON.parseFull(configJsonText).get.asInstanceOf[Map[String, String]]
      accessToken = Token(config.getOrElse("accessToken",       ""),
                          config.getOrElse("accessTokenSecret", ""))
      if (accessToken.key == "") {
        try {
          val token = Http("https://auth.opera.com/service/oauth/request_token").param("oauth_callback","oob").oauth(consumer).asToken

          println("Go to https://auth.opera.com/service/oauth/authorize?oauth_token=" + token.key)
          val verifier = Console.readLine("Enter verifier: ").trim

          accessToken = Http("https://auth.opera.com/service/oauth/access_token").oauth(consumer, token, verifier).asToken
          var outFile = new java.io.FileOutputStream(accessTokenFilePath)
          var outStream = new java.io.PrintStream(outFile)
          outStream.print("{\n" + "  \"accessToken\": \"" + accessToken.key +
                          "\",\n  \"accessTokenSecret\": \"" +
                          accessToken.secret + "\"\n}\n")
          outStream.close
        } catch {
          case e: scalaj.http.HttpException => println(e.body); System.exit(1)
        }
      }

      val linkApi = new LinkAPI(consumer, accessToken)
      val search_engines = linkApi.getSearchEngines()

      val engine = new TemplateEngine
      println(engine.layout("template.ssp",
                            Map("search_engines" -> search_engines)))
    }
  }
}
