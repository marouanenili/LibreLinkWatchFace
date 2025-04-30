package com.example.freestylelibrelink;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class GlucoseManager {
    private final Integer SENSIBILITY = 30;  // valeur de sensibilité à l'insuline en mg/dL
    private final Integer CIBLE = 120;
    private final Integer THRESHOLD_HIGH = 180;

    private ApiManager apiManager;
    public GlucoseManager(ApiManager apiManager){
        this.apiManager = apiManager;
    }
    public String getLatestValue(String token, String patientIdSha256, String patientId) throws JSONException, IOException {
        JSONObject response = apiManager.getGraphData(token, patientIdSha256, patientId);
        return response.getJSONObject("data").getJSONObject("connection").getJSONObject("glucoseMeasurement").getString("ValueInMgPerDl");
    }

    public String getCorrection(String latestValue){
        int value = Integer.parseInt(latestValue);
        if (value < THRESHOLD_HIGH) return "0";
        return String.valueOf((value - CIBLE)/SENSIBILITY);
    }
}
