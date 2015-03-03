package foorgol;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * HTTP utility.
 *
 * @author cchantep
 */
public class Http {
    
    /**
     * Returns HTTP client.
     */
    public static CloseableHttpClient client() {
        return HttpClientBuilder.create().build();
    } // end of client
} // end of class Http
