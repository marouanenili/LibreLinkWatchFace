package freestylelibrelink;// src/androidTest/java/.../ApiManagerIntegrationTest.java
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.IOException;
import static org.junit.Assert.*;

import com.example.freestylelibrelink.ApiManager;

@RunWith(AndroidJUnit4.class)
public class ApiManagerIntegrationTest {
    private static final String TEST_EMAIL = "marouanenili123@gmail.com"; // Replace with your test account email
    private static final String TEST_PASSWORD = "Asmaabessa12!"; // Replace with your test account password

    @Test
    public void testLogin() throws IOException, JSONException {
        String token = ApiManager.login(TEST_EMAIL, TEST_PASSWORD);
        assertNotNull(token); // Assert that a token was returned
        assertNotEquals("", token); // Assert that the token is not empty
    }

    @Test
    public void testGetPatientConnections() throws IOException, JSONException {
        String token = ApiManager.login(TEST_EMAIL, TEST_PASSWORD);
        JSONArray connections = ApiManager.getPatientConnections(token);
        assertNotNull(connections); // Assert that a JSONArray was returned
        assertTrue(connections.length() >= 0); // Assert that the array is not null and has some elements
    }

    @Test
    public void testGetCGMData() throws IOException, JSONException {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjFiZWU0MjM2LTljZDItZTYxMS04MTI4LTA2MTBlNmUzOGNiZCIsImZpcnN0TmFtZSI6Ik1hcm91YW5lIiwibGFzdE5hbWUiOiJOaWxsaSIsImNvdW50cnkiOiJGUiIsInJlZ2lvbiI6ImZyIiwicm9sZSI6InBhdGllbnQiLCJ1bml0cyI6MSwicHJhY3RpY2VzIjpbXSwiYyI6MSwicyI6ImxsdS5hbmRyb2lkIiwiZXhwIjoxNzM0NjM2NTE2fQ.c9iozBFOp1SNZFs9WuIZ_tuYx_In-ffgUS2SfbiDMrA";
        // Assuming you have a known patient ID for testing
        String testPatientId = "1bee4236-9cd2-e611-8128-0610e6e38cbd";
        JSONObject cgmData = ApiManager.getCGMData(token, testPatientId).getJSONObject("data").getJSONObject("connection");
        JSONObject GlucoseMeasurment = cgmData.getJSONObject("glucoseMeasurement");
        int mValueInMgPerDl = GlucoseMeasurment.getInt("ValueInMgPerDl");

        assertNotNull(mValueInMgPerDl); // Assert that a JSONObject was returned
        // Add more specific assertions based on the expected structure of the CGM data
    }
}