package org.aksw.deer.helper.prefixFinder;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

/**
 * Get mappings from prefix.cc
 * BE CAREFULL
 *      prefixes are not unique. This may cause errors in resulting knowlegebase
 *      prefixes with no Resolution will not be touched!
 * Created by wolfo on 16.06.15.
 */
public class PrefixFinder {
    final String prefixCCURL = "http://prefix.cc/";
    final String requestSuffix = ".file.json";

    /**
     * Get Resolution of prefix from prefix.cc
     * @param prefix String of prefix you want to get resoluted
     * @return  URL resolution of Prefix or null if nothing is found
     */
    public String getPrefixResolution (String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return null;
        }
        String returnURL;
        String buffer = "", line;
        // Send data
        URL url = null;
        try {
            url = new URL(prefixCCURL + prefix + requestSuffix);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Accept", "application/json");
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                buffer = buffer + line + "\n";
            }
            rd.close();
            //		Get HashMap out of json
            HashMap<String, String> mapping = new ObjectMapper().readValue(buffer, HashMap.class);
            returnURL = mapping.get(prefix);
        } catch (MalformedURLException e) {
            return null;
        } catch (IOException e) {
            return null;
        }

        return returnURL;
    }
}
