package prkn.oauth

import java.net.URLDecoder
import javax.servlet.annotation.WebServlet
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}

import scala.util.{Failure, Success, Try}

/**
  * Created by epkprkn on 2016-03-08.
  */
@WebServlet(Array("/o/oauth2/v2/auth"))
class AAServer extends HttpServlet {

  def urlDecode(s: String): String = URLDecoder.decode(s, "UTF-8")

  override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {

//    request.getAttribute()
    val qs = Option(request.getQueryString) match {
      case None => Map.empty[String, String]
      case Some(s) => s.split("&").map(_.split("=")).map(a => (urlDecode(a(0)), urlDecode(a(1)))).toMap
    }

    Try {
      //First round of required parameters
      val clientId = qs("client_id")
      val redirectURI = qs("redirect_uri")
      validateClient(clientId, redirectURI)
      secondStage(clientId, redirectURI)
    } recover {
      /*
       If the request fails due to a missing, invalid, or mismatching
      redirection URI, or if the client identifier is missing or invalid,
      the authorization server SHOULD inform the resource owner of the
      error and MUST NOT automatically redirect the user-agent to the
      invalid redirection URI.
       */
      case nse: NoSuchElementException =>
        response.sendError(400, nse.getMessage)
      case bce: BadClientExpection =>
        response.sendError(403, "Forbidden")
      case e: Exception =>
        response.sendError(400, e.toString)
    }

    def validateClient(clientId: String, redirectURI: String): Unit = {
      val result: Try[Boolean] = Success(true) //TODO: check that we know about the client, verify that the redirectURI is kept on record
      result match {
        case Success(b) =>
        case Failure(e) =>
          throw new BadClientExpection()
      }
    }


    //    val t = Try {
    //      1/0
    //    } recoverWith{
    //      case e:Exception => Failure(new IllegalAccessError())
    //    }
    //    t.flatMap {i => Try{i*2}}

    def secondStage(clientId: String, redirectURI: String) = {
      //Capture parameters
      val state = qs.get("state")
      val responseType = qs.get("response_type")
      //Optional parameters
      val scope: Option[Seq[String]] = qs.get("scope").map(_.split(" "))

      //Below are not part of standard, introduced by Google (?)
      val accessType = qs.get("access_type") match {
        case None => "online"
        case Some("online") => "online"
        case Some("offline") => "offline"
        case Some(_) => "online"
      }
      val prompt = qs.get("prompt")
      val loginHint = qs.get("login_hint")
      val includeGrantedScopes = qs.get("include_granted_scopes") match {
        case None => false
        case Some("true") => true
        case Some(_) => false
      }

      //Validate
      Try {
        require(responseType == Some("code"), "bad response_type")
        require(state.isDefined, "no state was provided")
        //TODO validate scopes

      } recover {
        case iae: IllegalArgumentException =>
          val error = "invalid_request"
          val errorDescription = iae.getMessage

          response.sendRedirect(redirectURI)
      }
    }
  }

  class BadClientExpection extends Exception

}
