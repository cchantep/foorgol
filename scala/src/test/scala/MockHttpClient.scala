package fr.applicius.foorgol

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase

import resource.{ ManagedResource, Resource, managed }

private[foorgol] class MockHttpClient(
  a: HttpRequestBase ⇒ HttpResponse,
  b: OAuthClient.Refreshable[_ <: HttpRequestBase] ⇒ (String, HttpResponse))
    extends HttpClient {

  implicit object MockResponseResource extends Resource[HttpResponse] {
    def close(resp: HttpResponse) = ()
  }

  implicit object RefreshedResponseResponse
      extends Resource[(String, HttpResponse)] {
    def close(resp: (String, HttpResponse)) = ()
  }

  /** Executes request and returns response. */
  def execute[R <: HttpRequestBase](request: R): ManagedResource[HttpResponse] =
    managed(a(request))

  /** Executes refreshable request and returns response. */
  def execute[R <: HttpRequestBase](request: OAuthClient.Refreshable[R]): ManagedResource[(String, HttpResponse)] = managed(b(request))

  def close() = ()
}

object MockHttpClient {
  def apply(a: HttpRequestBase ⇒ HttpResponse = { req ⇒ sys.error(s"Unsupported request: $req") }, b: OAuthClient.Refreshable[_ <: HttpRequestBase] ⇒ (String, HttpResponse) = { req ⇒ sys.error(s"Unsupported refreshable request: $req") }): MockHttpClient = new MockHttpClient(a, b)
}
