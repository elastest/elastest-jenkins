package jenkins.plugins.elastest;

import com.google.gson.JsonObject;

import jenkins.plugins.elastest.utils.RestClient;

public class EIMManager {
    final RestClient restClient;
    String apiUrl;

    public EIMManager(String apiUrl) {
        this.apiUrl = apiUrl;
        this.restClient = new RestClient();
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    /* *** Methods *** */

    public void sendPacketLossWithCron(String agentId, String packetLossValue,
            String cronExpression) throws Exception {
        if (apiUrl == null || agentId == null || packetLossValue == null) {
            throw new Exception("EIM API, SuT agent Id or packetLoss Value is null");
        }

        String url = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
        url += "agent/controllability/" + agentId + "/packetloss";

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("exec", "EXECBEAT");
        jsonBody.addProperty("component", "EIM");
        jsonBody.addProperty("packetLoss", packetLossValue);
        jsonBody.addProperty("stressNg", "");
        jsonBody.addProperty("dockerized", "yes");
        jsonBody.addProperty("cronExpression",
                cronExpression != null ? "@every " + cronExpression : "");

        restClient.sendPost(url, jsonBody.toString());
    }

    public void sendPacketLoss(String agentId, String packetLossValue) throws Exception {
        sendPacketLossWithCron(agentId, packetLossValue, null);
    }

    public void removePacketloss(String agentId) throws Exception {
        if (apiUrl == null || agentId == null) {
            throw new Exception("EIM API or Agent Id is null");
        }

        String url = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
        url += "agent/" + agentId + "/unchecked";

        restClient.delete(url);
    }

    public void sendCpuBurstWithCron(String agentId, String cpuBurstValue, String cronExpression)
            throws Exception {
        if (apiUrl == null || agentId == null || cpuBurstValue == null) {
            throw new Exception("EIM API, SuT agent Id or cpuBurst Value is null");
        }

        String url = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
        url += "agent/controllability/" + agentId + "/stress";

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("exec", "EXECBEAT");
        jsonBody.addProperty("component", "EIM");
        jsonBody.addProperty("packetLoss", "");
        jsonBody.addProperty("stressNg", cpuBurstValue);
        jsonBody.addProperty("dockerized", "yes");
        cronExpression = cronExpression != null ? cronExpression : "60s";
        jsonBody.addProperty("cronExpression", "@every " + cronExpression);

        restClient.sendPost(url, jsonBody.toString());
    }

    public void sendCpuBurst(String agentId, String cpuBurstValue) throws Exception {
        sendCpuBurstWithCron(agentId, cpuBurstValue, null);
    }

}
