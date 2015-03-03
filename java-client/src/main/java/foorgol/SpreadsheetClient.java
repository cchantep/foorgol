package foorgol;

import org.apache.http.client.methods.HttpGet;

/**
 * Feed client for Google spreadsheet.
 *
 * @author cchantep
 */
public class SpreadsheetClient {
    // --- Shared ---

    /**
     * Base URL
     * @value "https://spreadsheets.google.com/feeds"
     */
    public static final String BASE_URL = 
        "https://spreadsheets.google.com/feeds";

    // ---

    /**
     * Prepares request to get list of spreadsheet available 
     * for given access |token|.
     */
    public static HttpGet listRequest() {
        return new HttpGet(BASE_URL + "/spreadsheets/private/full");
    } // end of listRequest
} // end of class SpeadsheetClient
