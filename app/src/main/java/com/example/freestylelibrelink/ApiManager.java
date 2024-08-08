package com.example.freestylelibrelink;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

public class ApiManager {

    private static final String BASE_URL = "https://api-fr.libreview.io"; // Pour l'Europe, utiliser "https://api.libreview.io"
    private static final String LOGIN_ENDPOINT = "/llu/auth/login";
    private static final String CONNECTIONS_ENDPOINT = "/llu/connections";
    private static final String CGM_DATA_ENDPOINT = "/llu/connections/%s/graph";

    // Headers
    private static final String CONTENT_TYPE = "application/json";
    private static final String PRODUCT = "llu.android";
    private static final String VERSION = "4.7";

    // Function to log in and retrieve JWT token
    public static String login(String email, String password) throws IOException, JSONException {
        URL url = new URL(BASE_URL + LOGIN_ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("accept-encoding", "gzip");
        conn.setRequestProperty("cache-control", "no-cache");
        conn.setRequestProperty("connection", "Keep-Alive");
        conn.setRequestProperty("content-type", CONTENT_TYPE);
        conn.setRequestProperty("product", PRODUCT);
        conn.setRequestProperty("version", VERSION);
        conn.setDoOutput(true);

        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("email", email);
        jsonPayload.put("password", password);

        conn.getOutputStream().write(jsonPayload.toString().getBytes());

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONObject authTicket = jsonResponse.getJSONObject("data").getJSONObject("authTicket");
        return authTicket.getString("token");
    }

    // Function to get connections of patients
    public static JSONArray getPatientConnections(String token) throws IOException, JSONException {
        URL url = new URL(BASE_URL + CONNECTIONS_ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("accept-encoding", "gzip");
        conn.setRequestProperty("cache-control", "no-cache");
        conn.setRequestProperty("connection", "Keep-Alive");
        conn.setRequestProperty("content-type", CONTENT_TYPE);
        conn.setRequestProperty("product", PRODUCT);
        conn.setRequestProperty("version", VERSION);
        conn.setRequestProperty("Authorization", "Bearer " + token);

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return new JSONObject(response.toString()).getJSONArray("data");
    }

    // Function to retrieve CGM data for a specific patient
    public static JSONObject getCGMData(String token, String patientId) throws IOException, JSONException {
        URL url = new URL(BASE_URL + String.format(CGM_DATA_ENDPOINT, patientId));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("accept-encoding", "gzip");
        conn.setRequestProperty("cache-control", "no-cache");
        conn.setRequestProperty("connection", "Keep-Alive");
        conn.setRequestProperty("content-type", CONTENT_TYPE);
        conn.setRequestProperty("product", PRODUCT);
        conn.setRequestProperty("version", VERSION);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        InputStream inputStream;
        if ("gzip".equals(conn.getContentEncoding())) {
            inputStream = new GZIPInputStream(conn.getInputStream());
        } else {
            inputStream = conn.getInputStream();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return new JSONObject(response.toString());
    }


}
