# Foorgol Java

Low-level vanilla Java client for Google API.

## OAuth

> OAuth credentials are visible in [Google Developer Console](https://console.developers.google.com).

If an *offline token* as expired, you can refresh it.

```java
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;

import foorgol.OAuthClient;

DefaultHttpClient client = new DefaultHttpClient();

// If unauthorized response (401) as token is expired ...

CloseableHttpResponse resp = client.execute(
  OAuth.refreshRequest("clientId", "clientSecret", "refreshTok"));

String refreshedAccessToken = OAuthClient.parseRefreshResponse(resp);
```

This utility also provides automatic refresh for request.

```java
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;

import foorgol.OAuthClient;

OAuthClient.Refreshable<HttpPost> refreshable = 
  OAuthClient.refreshable(myPost, 
    "accessToken", // offline token, maybe expired
    "clientId", "clientSecret", "refreshTok" // use to refresh if expired);

DefaultHttpClient client = new DefaultHttpClient();

refreshable.executeWith(client);
// 1. Execute myPost prepared with initial accessToken.
// 2a. If response is successful (200), return it as result.
// 2b. If response is unauthorized (401), 
//     send a refresh request before retrying once.
```