package com.example.freestylelibrelink;

import com.example.freestylelibrelink.ApiManager;
import com.google.common.hash.Hashing;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CredentialsManager {
    String email;
    String password;
    String token;
    String accountId;
    String patientId;
    String patientIdSha256;
    ApiManager apiManager;

    public CredentialsManager(String email, String password) throws JSONException, IOException {
        this.email = email;
        this.password = password;
        this.apiManager = new ApiManager();

        JSONObject loginResponse = apiManager.getLoginResponse(email, password);
        this.token = loginResponse.getJSONObject("data").getJSONObject("authTicket").getString("token");

        String id = loginResponse.getJSONObject("data").getJSONObject("user").getString("id");
        this.patientId = id;
        this.patientIdSha256 = Hashing.sha256().hashString(id, StandardCharsets.UTF_8).toString();

        JSONObject connectionsResponse = apiManager.getConnectionsResponse(token, patientIdSha256);
        this.accountId = connectionsResponse.getJSONArray("data").getJSONObject(0).getString("id");
    }
    public String getToken() {
        return token;
    }
    public String getPatientId() {
        return patientId;
    }
    public String getAccountId() {
        return accountId;
    }
    public String getPatientIdSha256() {
        return patientIdSha256;
    }

}