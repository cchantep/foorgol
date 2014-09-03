package fr.applicius.foorgol;

import java.io.InputStreamReader;
import java.io.InputStream;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.apache.http.HttpResponse;

import org.apache.http.message.BasicNameValuePair;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.client.entity.UrlEncodedFormEntity;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * OAuth utility. 
 *
 * @author cchantep
 */
public final class OAuth {
    // --- Shared ---

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger("foorgol-java");

    // ---
    
    /**
     * Returns request prepared with headers required for Google OAuth.
     *
     * @param request Initial request
     * @param accessToken Access token previously obtained from Google OAuth
     */
    public static <R extends HttpRequestBase> R prepareRequest(final R request, final String accessToken) { 
        request.setHeader("Authorization", "Bearer " + accessToken);
        return request;
    } // end of prepareRequest

    /**
     * Returns request to refresh on offline access token from Google OAuth.
     *
     * @param clientId OAuth client ID (see Credentials in Google Dev Console)
     * @param clientSecret OAuth client secret
     * @param refreshToken Refresh token previously obtained along with offline access token.
     */
    public static HttpPost refreshRequest(final String clientId, 
                                          final String clientSecret,
                                          final String refreshToken) {

        final HttpPost req = 
            new HttpPost("https://accounts.google.com/o/oauth2/token");

        final ArrayList<BasicNameValuePair> ps = 
            new ArrayList<BasicNameValuePair>();

        ps.add(new BasicNameValuePair("client_id", clientId));
        ps.add(new BasicNameValuePair("client_secret", clientSecret));
        ps.add(new BasicNameValuePair("refresh_token", refreshToken));
        ps.add(new BasicNameValuePair("grant_type", "refresh_token"));

        try {
            req.setEntity(new UrlEncodedFormEntity(ps));
        } catch (java.io.UnsupportedEncodingException e) {
            logger.log(Level.SEVERE, "Fails to prepare refresh request", e);
            throw new RuntimeException("Fails to prepare request", e);
        } // end of catch

        return req;
    } // end of refreshRequest

    /**
     * Parses |response| to a refresh request.
     * Content will be consumed and closed.
     *
     * @param response Response to HTTP POST
     * @return Refreshed access token for Google OAuth
     * @see #refreshRequest
     * @throws IllegalArgumentException if response is null
     * @throws IllegalStateException if response is not successful (200)
     * @throws RuntimeException if response is successful but fails to parses it
     */
    public static String parseRefreshResponse(final HttpResponse response) {
        if (response == null) throw new IllegalArgumentException();

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IllegalStateException();
        } // end of if

        // ---
        
        JsonReader jr = null;

        try {
            final InputStream in = response.getEntity().getContent();
            jr = new JsonReader(new InputStreamReader(in, "UTF-8"));

            jr.beginObject(); // {

            String tok = null;

            while (jr.hasNext()) {
                final String name = jr.nextName();

                if ("token_type".equals(name)) {
                    final String t = jr.nextString();

                    if (!"Bearer".equals(t)) {
                        throw new RuntimeException("Unexpected token type: " + t);
                    }
                } else if ("access_token".equals(name)) {
                    tok = jr.nextString();
                } else {
                    // expires_in or unexpected property
                    jr.skipValue();                    
                } // end of else
            }
                    
            return tok;
        } catch (RuntimeException e) { 
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fails to parse refresh response", e);
            throw new RuntimeException("Fails to parse response", e);
        } finally {
            if (jr != null) {
                try {
                    jr.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } // end of catch
            } // end of if
        } // end of finally
    } // end of parseRefreshResponse
} // end of class OAuth
