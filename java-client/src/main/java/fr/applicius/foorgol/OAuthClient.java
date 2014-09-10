package fr.applicius.foorgol;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.Closeable;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.apache.commons.lang3.tuple.ImmutablePair;

import org.apache.http.HttpResponse;
import org.apache.http.Header;

import org.apache.http.message.BasicNameValuePair;

import org.apache.http.impl.client.CloseableHttpClient;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.client.entity.UrlEncodedFormEntity;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * OAuth client. 
 *
 * @author cchantep
 */
public class OAuthClient {
    // --- Shared ---

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger("foorgol-oauth");

    // ---
    
    /**
     * Returns request prepared with headers required for Google OAuth.
     *
     * @param request Initial request
     * @param accessToken Access token previously obtained from Google OAuth
     */
    public static <R extends HttpRequestBase> R prepare(final R request, final String accessToken) { 
        request.setHeader("Authorization", "Bearer " + accessToken);
        return request;
    } // end of prepare

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
     * Returns request as refreshable.
     *
     * @param unprepared Unprepared request ({#prepareRequest} not applied on)
     * @param accessToken Offline access token
     * @param clientId OAuth client ID (see Credentials in Google Dev Console)
     * @param clientSecret OAuth client secret
     * @param refreshToken Refresh token previously obtained along with offline access token.
     * @see #refreshRequest
     */
    public static <R extends HttpRequestBase> Refreshable<R> refreshable(final R unprepared, final String accessToken, final String clientId, final String clientSecret, final String refreshToken) {

        return new Refreshable<R>(unprepared, accessToken,
                                  clientId, clientSecret, refreshToken);

    } // end of refreshable

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

    // --- Inner classes ---

    /**
     * Refreshable request.
     */
    public static class Refreshable<R extends HttpRequestBase> { 
        // --- Properties ---

        /**
         * Underlying prepared request
         */
        final R underlying;

        /**
         * Access token
         */
        final String accessToken;

        /**
         * OAuth Client ID
         */
        final String clientId;

        /**
         * OAuth client secret
         */
        final String clientSecret;

        /**
         * Refresh token
         */
        final String refreshToken;

        // ---

        /**
         * @param unprepared Unprepared request ({OAuth#prepareRequest} not applied on).
         */
        Refreshable(final R unprepared, 
                    final String accessToken,
                    final String clientId,
                    final String clientSecret,
                    final String refreshToken) {

            this.accessToken = accessToken;
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.refreshToken = refreshToken;
            this.underlying = unprepared;
        } // end of <init>

        // ---

        /**
         * Executes underlying request with given |client|.
         * If first HTTP call returns 401 status code, it tries to refresh
         * access token.
         * (Given client is not closed)
         */
        public ImmutablePair<String, CloseableHttpResponse> executeWith(final CloseableHttpClient client) {
            CloseableHttpResponse resp = null;

            try {
                resp = client.
                    execute(prepare(this.underlying, this.accessToken));

                final int statusCode = resp.getStatusLine().getStatusCode();

                logger.log(Level.FINER, "Initial status code: {0}", statusCode);

                final Header contentLength = 
                    resp.getFirstHeader("Content-Length");

                final String len = (contentLength == null) ? "" 
                    : contentLength.getValue();

                final boolean successful = 
                    (statusCode >= 200 && statusCode < 300);

                if (successful && !"0".equals(len)) {
                    return ImmutablePair.of(this.accessToken, resp);
                } else if (successful || statusCode == 401) {
                    logger.warning("Will try to execute with refreshed token");

                    resp.close();

                    resp = client.execute(refreshRequest(clientId, clientSecret, refreshToken));

                    final String tok = parseRefreshResponse(resp);

                    resp.close();

                    logger.log(Level.FINE, "Refresh access token: {0}", tok);
                    
                    return ImmutablePair.
                        of(tok, client.execute(prepare(underlying, tok)));

                } // end of else if

                // ---

                throw new IllegalStateException("Unexpected status code: " + statusCode);
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new RuntimeException("Fails to execute refreshable request", e);
            } // end of catch
        } // end of execute
    }
} // end of class OAuth
