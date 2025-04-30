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

    // API endpoint URLs
    private static final String BASE_URL = "https://api-fr.libreview.io"; // Pour l'Europe, utiliser "https://api.libreview.io"
    private static final String LOGIN_ENDPOINT = "/llu/auth/login";
    private static final String CONNECTIONS_ENDPOINT = "/llu/connections";
    private static final String CGM_DATA_ENDPOINT = "/llu/connections/%s/graph";

    // Headers
    private static final String CONTENT_TYPE = "application/json";
    private static final String PRODUCT = "llu.android";
    private static final String VERSION = "4.12.0";


    // Function to log in and retrieve JWT token
    public JSONObject getLoginResponse(String email, String password) throws IOException, JSONException {
        URL url = new URL(BASE_URL + LOGIN_ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn = setHeader("POST", conn, null, null);

        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("email", email);
        jsonPayload.put("password", password);

        conn.getOutputStream().write(jsonPayload.toString().getBytes());

        // Check if response is GZIP-encoded
        InputStream inputStream;
        String encoding = conn.getContentEncoding();
        if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
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
        System.out.println("Response: " + response.toString());
        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse;
    }

    // Function to get connections of patients
    public JSONObject getConnectionsResponse(String token, String patientIdSha256) throws IOException, JSONException {
        URL url = new URL(BASE_URL + CONNECTIONS_ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn = setHeader("GET", conn, token, patientIdSha256);

        InputStream inputStream;
        if ("gzip".equals(conn.getHeaderField("Content-Encoding"))) {
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

    // Function to retrieve CGM data for a specific patient
    public JSONObject getGraphData(String token, String patientidSha256, String patientid) throws IOException, JSONException {
        URL url = new URL(BASE_URL + String.format(CGM_DATA_ENDPOINT, patientid));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn = setHeader("GET", conn, token, patientidSha256);
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

    private HttpURLConnection setHeader(String method, HttpURLConnection conn, String token, String accountId) throws IOException {
        conn.setRequestMethod(method);
        conn.setRequestProperty("accept-encoding", "gzip");
        conn.setRequestProperty("cache-control", "no-cache");
        conn.setRequestProperty("connection", "Keep-Alive");
        conn.setRequestProperty("content-type", CONTENT_TYPE);
        conn.setRequestProperty("product", PRODUCT);
        conn.setRequestProperty("version", VERSION);
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        if (accountId != null) {
            conn.setRequestProperty("account-id", accountId);
        }
        return conn;
    }

}