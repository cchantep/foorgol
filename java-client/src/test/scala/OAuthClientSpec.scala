package fr.applicius.foorgol

import org.apache.http.client.methods.{ HttpGet, HttpPost }
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.message.{ BasicHttpResponse, BasicStatusLine }

object OAuthClientSpec extends org.specs2.mutable.Specification {
  "OAuth client" title

  "Prepared request" should {
    "have authorization header" in {
      OAuthClient.prepare(httpGet, "toktok") aka "request" must beLike {
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
      OAuthClient.refreshRequest("clientId", "clientSecret", "reftok").
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
      OAuthClient.parseRefreshResponse(null).
        aka("parsing") must throwA[IllegalArgumentException]
    }

    "be refused if not successful" in {
      OAuthClient.parseRefreshResponse(refreshResponse1).
        aka("parsing") must throwA[IllegalStateException]

    }

    "not be parsed with unexpected token type" in {
      OAuthClient.parseRefreshResponse(refreshResponse2).
        aka("parsing") must throwA[RuntimeException](
          "Unexpected token type: Type2")

    }

    "be successful parsed" in {
      OAuthClient.parseRefreshResponse(refreshResponse3).
        aka("access token") must_== "1/fFBGRNJru1FQd44AzqT3Zg"
    }

    "be successful parsed ignoring unsupported property" in {
      OAuthClient.parseRefreshResponse(refreshResponse4).
        aka("access token") must_== "2/fFBGRNJru1FQd44AzqT3Zg"
    }
  }

  "Refreshable request" should {
    lazy val refreshableGet = OAuthClient.
      refreshable(httpGet, "accessTok", "clientId", "clientSecret", "refTok")

    "be refreshed on status code 401 (Unauthorized)" in {
      val reqs = scala.collection.mutable.MutableList.empty[String]
      lazy val client = MockClient { 
        case get: HttpGet => reqs += get.getURI.toString; refreshResponse5
        case post: HttpPost => reqs += post.getURI.toString; refreshResponse3
      }

      refreshableGet.executeWith(client) aka "response" must beLike {
        case resp =>
          reqs.toList aka "request URIs" mustEqual(List("http://localhost",
            "https://accounts.google.com/o/oauth2/token", "http://localhost"))

      }
    }

    "be refreshed on empty response (200 - zero-length)" in {
      val reqs = scala.collection.mutable.MutableList.empty[String]
      lazy val client = MockClient { 
        case get: HttpGet => reqs += get.getURI.toString; refreshResponse6
        case post: HttpPost => reqs += post.getURI.toString; refreshResponse3
      }

      refreshableGet.executeWith(client) aka "response" must beLike {
        case resp =>
          reqs.toList aka "request URIs" mustEqual(List("http://localhost",
            "https://accounts.google.com/o/oauth2/token", "http://localhost"))

      }
    }
  }

  // ---

  lazy val httpGet = new HttpGet("http://localhost")

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

  lazy val refreshResponse5 = new BasicHttpResponse(
      new BasicStatusLine(httpProto, 401, "Unauthorized"))

  lazy val refreshResponse6 = {
    val resp = new BasicHttpResponse(
      new BasicStatusLine(httpProto, 200, "OK"))
    resp.setHeader("Content-Length", "0")
    resp
  }
}

object MockClient {
  import org.apache.http.{ HttpHost, HttpRequest }
  import org.apache.http.protocol.HttpContext
  import org.apache.http.impl.client.CloseableHttpClient

  def apply[R](f: HttpRequest => BasicHttpResponse) =
    new CloseableHttpClient {
      def close() = ()
      def getConnectionManager() = ???
      def getParams() = ???

      def doExecute(h: HttpHost, r: HttpRequest, c: HttpContext) =
        MockResponse(f(r))
    }
}

object MockResponse {
  import org.apache.http.client.methods.CloseableHttpResponse
  import org.apache.http.{ Header, ProtocolVersion, StatusLine }

  def apply(r: BasicHttpResponse) = new CloseableHttpResponse {
    def close() = ()
    def addHeader(n: String, v: String) = r.addHeader(n, v)
    def addHeader(h: Header) = r.addHeader(h)
    def containsHeader(n: String) = r.containsHeader(n)
    def getAllHeaders() = r.getAllHeaders
    def getFirstHeader(n: String) = r.getFirstHeader(n)
    def getHeaders(n: String) = r.getHeaders(n)
    def getLastHeader(n: String) = r.getLastHeader(n)
    def getParams() = r.getParams()
    def getProtocolVersion() = r.getProtocolVersion
    def headerIterator(n: String) = r.headerIterator(n)
    def headerIterator() = r.headerIterator
    def removeHeader(h: Header) = r.removeHeader(h)
    def removeHeaders(n: String) = r.removeHeaders(n)
    def setHeader(n: String, v: String) = r.setHeader(n, v)
    def setHeader(h: Header) = r.setHeader(h)
    def setParams(ps: org.apache.http.params.HttpParams) = r.setParams(ps)
    def setHeaders(hs: Array[Header]) = r.setHeaders(hs)
    def getEntity() = r.getEntity
    def getLocale() = r.getLocale
    def getStatusLine() = r.getStatusLine()
    def setEntity(e: org.apache.http.HttpEntity) = r.setEntity(e)
    def setLocale(l: java.util.Locale) = r.setLocale(l)
    def setReasonPhrase(m: String) = r.setReasonPhrase(m)
    def setStatusCode(c: Int) = r.setStatusCode(c)
    def setStatusLine(l: StatusLine) = r.setStatusLine(l)
    def setStatusLine(p: ProtocolVersion, c: Int) = r.setStatusLine(p, c)
    def setStatusLine(p: ProtocolVersion, c: Int, m: String) =
      r.setStatusLine(p, c, m)

  }
}
