package com.JasonLattimer.Crm2011RESTJava;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.naming.ServiceUnavailableException;
import org.apache.commons.httpclient.util.URIUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CrmApplication {

    //This was registered in Azure AD as a WEB APPLICATION AND/OR WEB API
    //Azure Application Client ID
    private final static String CLIENT_ID = "00000000-0000-0000-0000-000000000000";
    //CRM URL
    private final static String RESOURCE = "https://orgname.crm.dynamics.com";
    //O365 credentials for authentication w/o login prompt
    private final static String USERNAME = "crmadmin@orgname.onmicrosoft.com";
    private final static String PASSWORD = "password";
    //Azure Directory OAUTH 2.0 AUTHORIZATION ENDPOINT minus "/oauth2/authorize"
    private final static String AUTHORITY = "https://login.microsoftonline.com/00000000-0000-0000-0000-000000000000";

    public static void main(String args[]) throws Exception {

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {

            //No prompt for credentials
            AuthenticationResult result = getAccessTokenFromUserCredentials();
            System.out.println("Access Token - " + result.getAccessToken());
            System.out.println("Refresh Token - " + result.getRefreshToken());
            System.out.println("ID Token - " + result.getIdToken());
            
            String newAccountId = CreateAccount(result.getAccessToken(), "Java Test", "1234");
            System.out.println("Created: " + newAccountId);
            
            String accountId = FindAccountByNumber(result.getAccessToken(), "1234");
            System.out.println("AccountId - " + accountId);

            String name = FindAccountname(result.getAccessToken(), accountId);
            System.out.println("Fullname: " + name);

            newAccountId = UpdateAccount(result.getAccessToken(), newAccountId);
            System.out.println("Updated: " + newAccountId);

            accountId = DeleteAccount(result.getAccessToken(), accountId);
            System.out.println("Deleted: " + accountId);
        }
    }

    private static String DeleteAccount(String token, String accountId) throws MalformedURLException, IOException {
        HttpURLConnection connection = null;
        URL url = new URL(RESOURCE + "/XRMServices/2011/OrganizationData.svc/AccountSet(guid'" + accountId + "')");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");
        connection.addRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.connect();

        int responseCode = connection.getResponseCode();
        
        //Nothing returned on delete

        return accountId;
    }

    private static String UpdateAccount(String token, String accountId) throws MalformedURLException, IOException, URISyntaxException, JSONException {
        JSONObject account = new JSONObject();
        account.put("WebSiteURL", "http://www.microsoft.com");

        HttpURLConnection connection = null;
        URL url = new URL(RESOURCE + "/XRMServices/2011/OrganizationData.svc/AccountSet(guid'" + accountId + "')");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("X-HTTP-Method", "MERGE");
        connection.setRequestProperty("Accept", "application/json; charset=utf-8");
        connection.addRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
        out.write(account.toString());
        out.flush();
        out.close();

        int responseCode = connection.getResponseCode();

        //Nothing returned on update
        
        return accountId;
    }

    private static String CreateAccount(String token, String name, String accountNumber) throws MalformedURLException, IOException, JSONException {
        JSONObject account = new JSONObject();
        account.put("Name", name);
        account.put("AccountNumber", accountNumber);

        HttpURLConnection connection = null;
        URL url = new URL(RESOURCE + "/XRMServices/2011/OrganizationData.svc/AccountSet");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setDoOutput(true);      
   
        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
        out.write(account.toString());
        out.flush();
        out.close();

        int responseCode = connection.getResponseCode();
        
        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        JSONObject jObject = new JSONObject(response.toString());
        JSONObject jD = (JSONObject) jObject.get("d");
        String accountId = jD.get("AccountId").toString();
        return accountId;
    }

    private static String FindAccountname(String token, String accountId) throws MalformedURLException, IOException, JSONException {
        HttpURLConnection connection = null;
        URL url = new URL(RESOURCE + "/XRMServices/2011/OrganizationData.svc/AccountSet(guid'" + accountId + "')?$select=Name");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json; charset=utf-8");
        connection.addRequestProperty("Authorization", "Bearer " + token);

        int responseCode = connection.getResponseCode();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject jObject = new JSONObject(response.toString());
        JSONObject jD = (JSONObject) jObject.get("d");
        String name = jD.get("Name").toString();
        return name;
    }

    private static String FindAccountByNumber(String token, String accountNumber) throws MalformedURLException, IOException, JSONException {
        HttpURLConnection connection = null;
        URL url = new URL(URIUtil.encodeQuery(RESOURCE + "/XRMServices/2011/OrganizationData.svc/AccountSet?$select=AccountId,Name&$filter=AccountNumber eq '" + accountNumber + "'"));
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Authorization", "Bearer " + token);

        int responseCode = connection.getResponseCode();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject jObject = new JSONObject(response.toString());
        JSONObject jD = (JSONObject) jObject.get("d");
        JSONArray jResults = (JSONArray) jD.get("results");
        JSONObject account = jResults.getJSONObject(0);
        String accountId = account.get("AccountId").toString();
        return accountId;
    }

    private static AuthenticationResult getAccessTokenFromUserCredentials()
            throws Exception {
        AuthenticationContext context = null;
        AuthenticationResult result = null;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext(AUTHORITY, false, service);
            Future<AuthenticationResult> future = context.acquireToken(RESOURCE,
                    CLIENT_ID,
                    USERNAME,
                    PASSWORD, null);
            result = future.get();
        } finally {
            service.shutdown();
        }

        if (result == null) {
            throw new ServiceUnavailableException("authentication result was null");
        }
        return result;
    }
}
