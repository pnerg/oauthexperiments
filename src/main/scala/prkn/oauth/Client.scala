package prkn.oauth

import java.net.{URLEncoder, URL}
import java.security.MessageDigest
import java.util.Base64
import javax.net.ssl.HttpsURLConnection
import javax.servlet.annotation.WebServlet
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpServlet}

import play.api.libs.json.Json

import scala.util.Random


/**
  * First point of entry
  */
@WebServlet(Array("/hejsan"))
class OAuthStart extends HttpServlet {

  //Config
  val outhProvider= "https://accounts.google.com/o/oauth2/v2/auth"

  val callbackUrl = "http://127.0.0.1:8080/OAuth2Callback"
  val encodedCBUrl = URLEncoder.encode(callbackUrl,"UTF-8")

  val clientID = ??? //provide your own

  override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    //This is just stuff to build a decently random "state", will be bounced back from the AuthServer
    val rnd = scala.collection.mutable.ArrayBuffer.fill[Byte](10)(0).toArray
    Random.nextBytes(rnd)
    val md = MessageDigest.getInstance("SHA-256")
    md.update(rnd)
    val state = Base64.getEncoder.withoutPadding.encodeToString(md.digest)
    request.getSession.setAttribute("oauthstate",state)

    //Redirect the user over to Google, ask for the "scopes" needed, provide the return address, and give our client id
    val oURL = s"""$outhProvider?scope=email%20profile&state=$state&redirect_uri=$encodedCBUrl&response_type=code&client_id=$clientID"""
    response.sendRedirect(oURL)
  }
}

//https://developers.google.com/identity/protocols/OAuth2WebServer

// This is where we land after a the user is authenticated towards Google
@WebServlet(Array("/OAuth2Callback"))
class OAuthLanding extends HttpServlet {

  val accessTokenURL = "https://www.googleapis.com/oauth2/v4/token"
  val callbackUrl = "http://127.0.0.1:8080/OAuth2Callback"
  val clientID = ??? //provide your own
  val clientSecret = ??? //provide your own

  override def doGet(request: HttpServletRequest, response: HttpServletResponse) = {
    val out = response.getWriter

    //Extract params out of the query url
    val qs = request.getQueryString.split("&").map(_.split("=")).map(a=> (a(0) -> a(1))).toMap
    out.println(qs)
    //TODO validate that we have a success
    val stateIn = qs("state")
    val code = qs("code")

    //TODO: validate state as [N]OK
    val stateStored = request.getSession.getAttribute("oauthstate")
    out.println(s"storedState: $stateStored")
    out.println(s"inState: $stateIn")
    out.println(stateStored == stateIn)

    //Exchange code for access token and ID token
    val body = s"code=$code&client_id=$clientID&client_secret=$clientSecret&redirect_uri=$callbackUrl&grant_type=authorization_code"
    val con = new URL(accessTokenURL).openConnection().asInstanceOf[HttpsURLConnection]
    con.setDoOutput(true)
    con.setRequestMethod("POST")
    con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
    con.getOutputStream.write(body.getBytes("UTF-8"))
    val res = scala.io.Source.fromInputStream(con.getInputStream).mkString
    out.println("=============")
    out.println(res)
    out.println("=============")

    // Parse out the access token from the response
    val js = Json.parse(res)
    val accessToken = (js \ "access_token").as[String]
    val tokenType = (js \ "token_type").as[String]
    val expiresIn = (js \ "expires_in").as[Int]
    val idToken = (js \ "id_token").as[String]
    out.println(accessToken)
    out.println(tokenType)
    out.println(expiresIn)
    out.println(idToken)

    //Digest the "id token", will contain basic profile of the user
    val idt = idToken.split("""\.""")
    val id = Json.parse(java.util.Base64.getDecoder.decode(idt(1)))
    out.println("=============")
    out.println(Json.prettyPrint(id))
    out.println("=============")

  }

}

