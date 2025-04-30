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
import com.example.freestylelibrelink.CredentialsManager;
import com.example.freestylelibrelink.GlucoseManager;

@RunWith(AndroidJUnit4.class)
    public class ApiManagerIntegrationTest {
    private static final String TEST_EMAIL = "test"; // Replace with your test account email
    private static final String TEST_PASSWORD = "test"; // Replace with your test account password
    private ApiManager apiManager;
    private CredentialsManager credentialsManager;

    @Test
    public void ApiManagerIntegrationTest() throws JSONException, IOException {
        apiManager = new ApiManager();
        credentialsManager = new CredentialsManager(TEST_EMAIL, TEST_PASSWORD);
        assertNotNull(credentialsManager.getAccountId());
        assertNotNull(credentialsManager.getPatientId());
        assertNotNull(credentialsManager.getToken());

    }

    @Test
    public void GlucoseManagerTest() throws JSONException, IOException {
        apiManager = new ApiManager();
        GlucoseManager glucoseManager = new GlucoseManager(apiManager);
        credentialsManager = new CredentialsManager(TEST_EMAIL, TEST_PASSWORD);
        String latestValue = glucoseManager.getLatestValue(credentialsManager.getToken(), credentialsManager.getPatientIdSha256(), credentialsManager.getPatientId());
        assertNotNull(latestValue);
        System.out.println("Latest Value: " + latestValue);
        assertTrue("latestValue should contain only digits", latestValue.matches("\\d+"));

        String correction = glucoseManager.getCorrection(latestValue);
        assertNotNull(correction);
        System.out.println("Correction: " + correction);
        assertTrue("correction should contain only digits", correction.matches("\\d+"));

    }

}