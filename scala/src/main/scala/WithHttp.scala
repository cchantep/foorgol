package fr.applicius.foorgol

import java.net.URI

import org.apache.http.{ HttpResponse, NameValuePair }
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicNameValuePair
import org.apache.http.client.methods.{ HttpGet, HttpPost, HttpRequestBase }
import org.apache.http.client.utils.URIBuilder

import resource.ManagedResource

/** HTTP client abstraction. */
trait HttpClient extends java.io.Closeable {
  import scala.collection.JavaConverters.seqAsJavaListConverter

  /** Executes request and returns response. */
  def execute[R <: HttpRequestBase](request: R): ManagedResource[HttpResponse]

  /** Executes refreshable request and returns response. */
  def execute[R <: HttpRequestBase](request: OAuthClient.Refreshable[R]): ManagedResource[HttpResponse]

  /** Executes a GET request and returns its response. */
  def get(uri: URI, parameters: (String, String)*) =
    parameters.headOption.fold(new HttpGet(uri)) { _ ⇒
      val builder = new URIBuilder(uri)
      builder.addParameters(parameters.map(p ⇒
        new BasicNameValuePair(p._1, p._2): NameValuePair).asJava)
      new HttpGet(builder.build)
    }

  /** Executes a POST request and returns its response. */
  def post(uri: URI, body: String, mimeType: String) = {
    val ent = new StringEntity(body, mimeType, "UTF-8")
    val post = new HttpPost(uri)
    post.setEntity(ent)
    post
  }
}

/** Client companion. */
object HttpClient {
  /** Returns new client using default implementation. */
  def apply(): HttpClient = new Default()

  /** Internal default implementation */
  private class Default extends HttpClient {
    import resource.managed
    val underlying = Http.client

    def execute[R <: HttpRequestBase](request: R): ManagedResource[HttpResponse] = managed(underlying execute request)

    def execute[R <: HttpRequestBase](request: OAuthClient.Refreshable[R]): ManagedResource[HttpResponse] = managed(request executeWith underlying)

    def close(): Unit = underlying.close()
  }
}

/** HTTP commons functions. */
trait WithHttp {
  /** Returns a new HTTP client. */
  def client: ManagedResource[HttpClient]
}
