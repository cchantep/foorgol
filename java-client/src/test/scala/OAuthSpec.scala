package fr.applicius.foorgol

import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.message.{ BasicHttpResponse, BasicStatusLine }

object OAuthSpec extends org.specs2.mutable.Specification {
  "OAuth" title

  "Prepared request" should {
    "have authorization header" in {
      OAuth.prepareRequest(httpGet, "toktok") aka "request" must beLike {
        case prepared => prepared.getLastHeader("Authorization").
            aka("authorization header") must beLike {
              case header =>
                header.getValue aka "header value" must_== "Bearer toktok"
            }
      }
    }
  }

  "Refresh request" should {
    "be expected URL encoded form one" in {
      OAuth.refreshRequest("clientId", "clientSecret", "reftok").
        aka("refresh request") must beLike {
          case req => req.getEntity aka "HTTP entity" must beLike {
            case ent: UrlEncodedFormEntity => 
              val buf = new java.io.ByteArrayOutputStream()
              ent.writeTo(buf)
              val params = new String(buf.toByteArray)

              params aka "parameters" must_== "client_id=clientId&client_secret=clientSecret&refresh_token=reftok&grant_type=refresh_token"
          }
        }
    }
  }

  "Refresh response" should {
    "be refused if null" in {
      OAuth.parseRefreshResponse(null).
        aka("parsing") must throwA[IllegalArgumentException]
    }

    "be refused if not successful" in {
      OAuth.parseRefreshResponse(refreshResponse1).
        aka("parsing") must throwA[IllegalStateException]

    }

    "not be parsed with unexpected token type" in {
      OAuth.parseRefreshResponse(refreshResponse2).
        aka("parsing") must throwA[RuntimeException](
          "Unexpected token type: Type2")

    }

    "be successful parsed" in {
      OAuth.parseRefreshResponse(refreshResponse3).
        aka("access token") must_== "1/fFBGRNJru1FQd44AzqT3Zg"
    }

    "be successful parsed ignoring unsupported property" in {
      OAuth.parseRefreshResponse(refreshResponse4).
        aka("access token") must_== "2/fFBGRNJru1FQd44AzqT3Zg"
    }
  }

  // ---

  lazy val httpGet = 
    new org.apache.http.client.methods.HttpGet("http://localhost")

  val httpProto = new org.apache.http.ProtocolVersion("http", 1, 1)

  lazy val refreshResponse1 = new BasicHttpResponse(
    new BasicStatusLine(httpProto, 404, "Not Found"))

  lazy val refreshResponse2 = {
    val resp = new BasicHttpResponse(
      new BasicStatusLine(httpProto, 200, "OK"))

    resp.setEntity(new org.apache.http.entity.StringEntity("""{
  "access_token":"1/fFBGRNJru1FQd44AzqT3Zg",
  "expires_in":3920,
  "token_type":"Type2"
}"""))

    resp
  }

  lazy val refreshResponse3 = {
    val resp = new BasicHttpResponse(
      new BasicStatusLine(httpProto, 200, "OK"))

    resp.setEntity(new org.apache.http.entity.StringEntity("""{
  "access_token":"1/fFBGRNJru1FQd44AzqT3Zg",
  "expires_in":3920,
  "token_type":"Bearer"
}"""))

    resp
  }

  lazy val refreshResponse4 = {
    val resp = new BasicHttpResponse(
      new BasicStatusLine(httpProto, 200, "OK"))

    resp.setEntity(new org.apache.http.entity.StringEntity("""{
  "access_token":"2/fFBGRNJru1FQd44AzqT3Zg",
  "expires_in":3920,
  "token_type":"Bearer",
  "extra": 1
}"""))

    resp
  }
}
