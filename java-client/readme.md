# Foorgol Java

Low-level vanilla Java client for Google API.

## OAuth

> OAuth credentials are visible in [Google Developer Console](https://console.developers.google.com).

If an *offline token* as expired, you can refresh it.

```java
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;

import fr.applicius.foorgol.OAuth;

DefaultHttpClient client = new DefaultHttpClient();

// If unauthorized response (401) as token is expired ...

final CloseableHttpResponse resp = client.execute(
  OAuth.refreshRequest("clientId", "clientSecret", "refreshTok"));

final String refreshedAccessToken = OAuth.parseRefreshResponse(resp);
```