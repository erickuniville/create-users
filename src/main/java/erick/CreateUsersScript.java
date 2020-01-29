package erick;

import okhttp3.*;
import org.apache.commons.lang.time.StopWatch;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import static java.util.UUID.randomUUID;
import static okhttp3.Request.Builder;

public class CreateUsersScript {
    //=========================================================================
    //                        PREENCHA ESTES PARÂMETROS
    //=========================================================================

    private static final String ENVIRONMENT = "https://app.customerfi.com";
    private static final String COMPANY_ID = "COMPANY_ID_HERE";
    private static final String ADMIN_USER_NAME = "ADMIN_USER_NAME_HERE";
    private static final String ADMIN_USER_PASSWORD = "ADMIN_PASSWORD_HERE";
    private static final String ADMIN_USER_DOMAIN = "ADMIN_COMPANY_SUBDOMAIN_HERE";
    private static final int HOW_MANY_TO_CREATE = 300_000;

    //=========================================================================
    //                  NÃO ALTERE O CÓDIGO DAQUI PARA BAIXO
    //=========================================================================

    private static final String ACCESS_TOKEN_URL = ENVIRONMENT + "/api/auth/v1/token";
    private static final String CREATE_USER_URL = ENVIRONMENT + "/api/core/v1/companies/{COMPANY_ID}/users";
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final JSONParser JSON_PARSER = new JSONParser();
    private static final StopWatch STOP_WATCH = new StopWatch();

    private static String accessToken = "";

    public static void main(String[] args) throws Exception {
        accessToken = requestNewAccessToken();
        for (int i = 0; i < HOW_MANY_TO_CREATE; i++) {
            createUser();
        }
    }

    private static void createUser() {
        STOP_WATCH.start();
        try (Response response = CLIENT.newCall(buildCreateUserRequest()).execute()) {
            STOP_WATCH.stop();
            boolean success = validateResponse(response);
            if (success) {
                logUserCreationAndElapsedTime(response);
            }
        } catch (Exception e) {
            printStrackTrace(e);
        } finally {
            STOP_WATCH.reset();
        }
    }

    private static void logUserCreationAndElapsedTime(Response response) throws Exception {
        String createdUserFirstName = extractFirstNameFromReturnedJson(response);
        long durationMillis = STOP_WATCH.getTime();
        System.out.println(createdUserFirstName + " - " + durationMillis + " ms");
    }

    private static String extractFirstNameFromReturnedJson(Response response) throws Exception {
        JSONObject json = extractJsonObject(response);
        return json.get("firstName").toString();
    }

    private static boolean validateResponse(Response response) throws Exception {
        if (response.isSuccessful()) {
            return true;
        }
        if (response.code() == 401) {
            accessToken = requestNewAccessToken();
            return false;
        }
        System.out.println("[ERROR] - HTTP Status Code: " + response.code());
        return false;
    }

    private static Request buildCreateUserRequest() {
        RequestBody body = RequestBody.create(JSON_MEDIA, buildUserJson());

        return new Builder()
                .url(CREATE_USER_URL.replace("{COMPANY_ID}", COMPANY_ID))
                .post(body)
                .addHeader("accept", "application/json, text/plain, */*")
                .addHeader("authorization", "Bearer " + accessToken)
                .build();
    }

    private static String buildUserJson() {
        String randomNumber = createRandomString();
        JSONObject user = new JSONObject();
        user.put("firstName", "user_" + randomNumber);
        user.put("lastName", "test");
        user.put("emailAddress", "user_" + randomNumber + "@created.by.java.script.com");
        user.put("initialStatus", "ACTIVATED");
        user.put("role", "USER");
        user.put("password", "Totvs@123");
        return user.toJSONString();
    }

    private static String createRandomString() {
        return randomUUID().toString().replaceAll("\n", "").substring(0, 6);
    }

    private static String requestNewAccessToken() throws Exception {
        System.out.println("Requesting new access token.");
        try (Response response = CLIENT.newCall(buildAccessTokenRequest()).execute()) {
            JSONObject json = extractJsonObject(response);
            return json.get("access_token").toString();
        }
    }

    private static Request buildAccessTokenRequest() {
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "password")
                .add("username", ADMIN_USER_NAME)
                .add("password", ADMIN_USER_PASSWORD)
                .add("domain", ADMIN_USER_DOMAIN)
                .build();
        return new Builder()
                .url(ACCESS_TOKEN_URL)
                .post(formBody)
                .build();
    }

    private static JSONObject extractJsonObject(Response response) throws Exception {
        String json = response.body().string();
        return (JSONObject) JSON_PARSER.parse(json);
    }

    private static void printStrackTrace(Exception e) {
        System.out.println();
        e.printStackTrace();
        System.out.println();
    }
}
