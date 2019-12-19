/*
 * The MIT License
 *
 * (C) Copyright 2017-2019 ElasTest (http://elastest.io/)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.elastest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.tasks.LogRotator;
import jenkins.model.BuildDiscarder;
import jenkins.plugins.elastest.json.ElasTestBuild;
import jenkins.plugins.elastest.json.ExternalJob;
import jenkins.plugins.elastest.json.ExternalJob.ExternalJobStatusEnum;
import jenkins.plugins.elastest.json.Sut;
import jenkins.plugins.elastest.json.TestSupportServices;
import jenkins.plugins.elastest.pipeline.ElasTestStep;
import jenkins.plugins.elastest.utils.Authenticator;

/**
 * Service to communicate with ElasTest and store the info related to each TJob
 * execution by build.
 * 
 * @author Francisco R. Díaz
 * @since 0.0.1
 */
public class ElasTestService implements Serializable {
    private static final long serialVersionUID = 1;
    private static final Logger LOG = LoggerFactory.getLogger(ElasTestService.class);
    private static ElasTestService instance;

    private static final String EIM_API_KEY = "ET_EIM_API";
    private static final String EIM_PACKETLOSS_KEY = "ET_EIM_CONTROLLABILLITY_PACKETLOSS";
    private static final String EIM_CPUBURST_KEY = "ET_EIM_CONTROLLABILLITY_CPUBURST";

    private HashMap<String, ElasTestBuild> elasTestBuilds;
    private Map<String, String> tSServicesCatalog;
    private String elasTestTJobApiUrl;
    private String elasTestVersionApiUrl;
    private String elasTestUrl;
    private transient Client client;
    protected boolean witAuthentication;
    private Authenticator authenticator;

    public ElasTestService() {
        this.elasTestBuilds = new HashMap<>();
        witAuthentication = false;
        client = ClientBuilder.newClient();
        authenticator = new Authenticator(null, null);
        setNewConfiguration();
    }

    private void setNewConfiguration() {
        elasTestUrl = ElasTestInstallation.getLogstashDescriptor().elasTestUrl;
        elasTestTJobApiUrl = elasTestUrl + "/api/external/tjob";
        elasTestVersionApiUrl = "/api/external/elastest/version";
        String name = ElasTestInstallation.getLogstashDescriptor().username;
        String password = ElasTestInstallation.getLogstashDescriptor().password;
        if ((name != null && !name.equals("")) && (password != null && !password.equals(""))) {
            witAuthentication = true;
            authenticator.setCredentials(name, password);
            if (client.getConfiguration().isRegistered(Authenticator.class)) {
                LOG.info("[elastest-plugin]: There is an Authenticator registered");
                LOG.info("[elastest-plugin]: Setting new credentials");
            } else {
                client = client.register(authenticator);
            }
            LOG.info("[elastest-plugin]: Now access to ElasTest is with username and password.");
        } else {
            LOG.info("[elastest-plugin]: Removing credentials");
            witAuthentication = false;
            authenticator.setCredentials(null, null);
            LOG.info("[elastest-plugin]: Now access to ElasTest is without username and password.");
        }
        tSServicesCatalog = loadTSSCatalog();
    }

    private Map<String, String> loadTSSCatalog() {
        Map<String, String> tSSCatalog = new HashMap<>();
        tSSCatalog.put("EUS", "29216b91-497c-43b7-a5c4-6613f13fa0e9");
        tSSCatalog.put("EBS", "a1920b13-7d11-4ebc-a732-f86a108ea49c");
        tSSCatalog.put("EMS", "bab3ae67-8c1d-46ec-a940-94183a443825");
        tSSCatalog.put("ESS", "af7947d9-258b-4dd1-b1ca-17450db25ef7");
        tSSCatalog.put("EDS", "fe5e0531-b470-441f-9c69-721c2b4875f2");
        return tSSCatalog;
    }

    // For normal Jenkins Job
    public void asociateToElasTestTJob(Run<?, ?> build, ElasTestBuildWrapper elasTestBuilder,
            ElasTestBuild elasTestBuild) throws Exception {
        LOG.info("[elastest-plugin]: Associate a Job to a TJob {}",
                build.getParent().getDisplayName());
        ExternalJob externalJob = new ExternalJob(build.getParent().getDisplayName());
        if (elasTestBuilder.isEus()) {
            List<String> tss = new ArrayList<>();
            tss.add("EUS");
            externalJob.setTSServices(prepareTSSToSendET(tss));
        }

        externalJob = asociateToElasTestTJob(externalJob);
        elasTestBuild.setExternalJob(externalJob);
        elasTestBuilds.put(build.getFullDisplayName(), elasTestBuild);
    }

    // For Pipeline
    public void asociateToElasTestTJob(Run<?, ?> build, ElasTestStep elasTestStep,
            ElasTestBuild elasTestBuild) throws Exception {
        ExternalJob externalJob = new ExternalJob(build.getParent().getDisplayName());

        Long maxBuilds = getMaxBuildsToKeep(build);
        if (maxBuilds != null && maxBuilds >= 0) {
            LOG.debug("Max builds to keep: {}", maxBuilds);
            externalJob.setMaxExecutions(maxBuilds);
        } else {
            LOG.debug("Max builds to keep: Using the default value set by elastest");
        }

        externalJob.setTSServices(prepareTSSToSendET(elasTestStep.getTss()));
        LOG.debug(
                "[elastest-plugin]: TestResutlPatter: " + elasTestStep.getSurefireReportsPattern());
        externalJob.setTestResultFilePattern((elasTestStep.getSurefireReportsPattern() != null
                && !elasTestStep.getSurefireReportsPattern().isEmpty())
                        ? elasTestStep.getSurefireReportsPattern()
                        : null);
        externalJob.setSut(elasTestStep.getSut() != -1L
                ? new Sut(elasTestStep.getSut(), elasTestStep.getSutParams())
                : null);
        externalJob.setFromIntegratedJenkins(elasTestStep.envVars.get("INTEGRATED_JENKINS") != null
                && elasTestStep.envVars.get("INTEGRATED_JENKINS").equals(Boolean.TRUE.toString())
                && elasTestUrl.equals("http://etm:8091"));
        LOG.info("[elastest-plugin]: Build URL: {}", elasTestStep.envVars.get("BUILD_URL"));
        LOG.info("[elastest-plugin]: Job URL: {}", elasTestStep.envVars.get("JOB_URL"));
        externalJob.setBuildUrl(elasTestStep.envVars.get("BUILD_URL"));
        externalJob.setJobUrl(elasTestStep.envVars.get("JOB_URL"));
        externalJob.setProject(
                !elasTestStep.getProject().isEmpty() ? elasTestStep.getProject() : null);
        externalJob = asociateToElasTestTJob(externalJob);
        elasTestBuild.setExternalJob(externalJob);
        elasTestBuild.setEnvVars(elasTestStep.envVars);
        LOG.info("[elastest-plugin]: Job associated with a TJob");
        elasTestBuilds.put(build.getFullDisplayName(), elasTestBuild);
        LOG.info("[elastest-plugin]: ElasTestBuild saved {} ", elasTestBuild);
    }

    public ExternalJob asociateToElasTestTJob(ExternalJob externalJob) throws Exception {
        int maxAttempts = 5;
        externalJob.settJobExecId(0L);
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            LOG.debug("[elastest-plugin]: associating with a TJob, attempt {}", attempt);
            try {
                if (attempt > 0) {
                    Thread.sleep(500);
                }
                externalJob = createTJobOnElasTest(externalJob);
                break;
            } catch (IllegalArgumentException | InterruptedException ie) {
                LOG.warn("[elastest-plugin]: {}", ie.getMessage());
            } catch (Exception e) {
                LOG.error("[elastest-plugin]: Error during reattempt -> {}", e.getMessage());
                if (attempt == maxAttempts - 1) {
                    throw e;
                }
            }
        }
        externalJob.setExecutionUrl(externalJob.getExecutionUrl());
        externalJob.setLogAnalyzerUrl(externalJob.getLogAnalyzerUrl());
        LOG.info("Content of the external Job returned by ElasTest.");
        return externalJob;
    }

    private ExternalJob createTJobOnElasTest(ExternalJob externalJob) throws Exception {
        ObjectMapper objetMapper = new ObjectMapper();
        WebTarget webTarget = client.target(elasTestTJobApiUrl);

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
        Response response = null;
        try {
            response = invocationBuilder
                    .post(Entity.entity(externalJob.toJSON(), MediaType.APPLICATION_JSON));
            externalJob = objetMapper.readValue(response.readEntity(String.class),
                    ExternalJob.class);
            if (externalJob.getStatus() == ExternalJobStatusEnum.ERROR) {
                throw new Exception(externalJob.getError());
            }
            LOG.debug("[elastest-plugin]: Body in association request: {}", externalJob.toJSON());
        } catch (Exception e) {
            LOG.error("[elastest-plugin]: Error trying to create a TJob {} in ElasTest: {}",
                    externalJob.toJSON(), e.getMessage());
            LOG.error("[elastest-plugin]: Elastest endpoint -> {}", elasTestTJobApiUrl);
            e.printStackTrace();
            throw e;
        }
        return externalJob;
    }

    public ExternalJob isReadyTJobForExternalExecution(ExternalJob externalJob) throws Exception {
        ObjectMapper objetMapper = new ObjectMapper();
        WebTarget webTarget = client.target(elasTestTJobApiUrl)
                .path(externalJob.gettJobExecId().toString());

        LOG.info("[elastest-plugin]: URL to check if a TJob is ready -> {}",
                webTarget.getUri().toString());

        Invocation.Builder invocationBuilder = webTarget.request()
                .accept(MediaType.APPLICATION_JSON);
        Response response = null;
        try {
            response = invocationBuilder.get(Response.class);
            externalJob = objetMapper.readValue(response.readEntity(String.class),
                    ExternalJob.class);
            if (externalJob.getStatus() == ExternalJobStatusEnum.ERROR) {
                throw new Exception(externalJob.getError());
            }
        } catch (Exception e) {
            LOG.error("[elastest-plugin]: Error cheking if the TJob is ready: {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return externalJob;
    }

    public void finishElasTestTJobExecution(ExternalJob externalJob) {
        LOG.info("[elastest-plugin]: Sending finalization message.");
        WebTarget webTarget = client.target(elasTestTJobApiUrl);

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
        try {
            invocationBuilder.put(Entity.entity(externalJob.toJSON(), MediaType.APPLICATION_JSON));

        } catch (Exception e) {
            LOG.error("[elastest-plugin]: Error sending the finalization message to ElasTest: {}",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public String getElasTestVersion() {
        String result = "KO";
        WebTarget webTarget = client.target(elasTestUrl).path(elasTestVersionApiUrl);

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN);
        Response response = null;
        try {
            response = invocationBuilder.get(Response.class);
            result = response.readEntity(String.class);
            LOG.info("[elastest-plugin]: ElasTest version installed: " + result);
        } catch (Exception uie) {
            LOG.error("[elastest-plugin]: Error invoking ElasTest.");
            result = "The connection to ElasTest could not be established.";
            throw uie;
        }
        return result;
    }

    public ExternalJob getExternalJobByBuildFullName(String buildFullName) {
        return elasTestBuilds.get(buildFullName).getExternalJob();
    }

    public HashMap<String, ElasTestBuild> getElasTestBuilds() {
        return elasTestBuilds;
    }

    public void removeExternalJobs(String buildId) {
        elasTestBuilds.remove(buildId);
    }

    public static synchronized ElasTestService getInstance() {
        if (instance == null) {
            instance = new ElasTestService();
        } else {
            instance.setNewConfiguration();
        }
        return instance;
    }

    private List<TestSupportServices> prepareTSSToSendET(List<String> tSServices) {
        List<TestSupportServices> eTTSServices = new ArrayList<>();
        for (String tSSName : tSServices) {
            if (tSServicesCatalog.containsKey(tSSName)) {
                TestSupportServices newTSService = new TestSupportServices(
                        tSServicesCatalog.get(tSSName), tSSName, true);
                eTTSServices.add(newTSService);
            }
        }
        return eTTSServices;
    }

    private Long getMaxBuildsToKeep(Run<?, ?> build) {
        BuildDiscarder buildDiscarder = build.getParent().getBuildDiscarder();
        if (buildDiscarder != null && buildDiscarder instanceof LogRotator) {
            return (long) ((LogRotator) buildDiscarder).getNumToKeep();
        }
        return null;
    }

    public String manageEIMIfNecessary(Run<?, ?> build, EnvVars etBuildVars) {
        try {
            EnvVars buildVars = build.getEnvironment();

            String EIM_AGENTID_KEY = "ET_EIM_SUT_AGENT_ID";
            if (etBuildVars.containsKey(EIM_API_KEY) && etBuildVars.containsKey(EIM_AGENTID_KEY)
                    && (buildVars.containsKey(EIM_PACKETLOSS_KEY)
                            || buildVars.containsKey(EIM_CPUBURST_KEY))) {
                String eimApiUrl = etBuildVars.get(EIM_API_KEY);
                EIMManager eimManager = new EIMManager(eimApiUrl);

                String agentId = etBuildVars.get(EIM_AGENTID_KEY);

                // Packetloss
                if (buildVars.containsKey(EIM_PACKETLOSS_KEY)) {
                    String packetLossValue = buildVars.get(EIM_PACKETLOSS_KEY);
                    LOG.info("Sending packet loss {} to agent {} through EIM at {}",
                            packetLossValue, agentId, eimApiUrl);
                    eimManager.sendPacketLoss(agentId, packetLossValue);
                }

                // Cpu burst
                if (buildVars.containsKey(EIM_CPUBURST_KEY)) {
                    String cpuBurstValue = buildVars.get(EIM_CPUBURST_KEY);
                    LOG.info("Sending cpu burst {} to agent {} through EIM at {}", cpuBurstValue,
                            eimApiUrl);
                    eimManager.sendCpuBurst(agentId, cpuBurstValue);
                }
                return agentId;
            }
        } catch (Exception e) {
            LOG.warn("[elastest-plugin] EIM manage: {}", e.getMessage());
        }

        return null;
    }

    public void manageEIMEndIfNecessary(Run<?, ?> build, EnvVars etBuildVars) {
        try {
            EnvVars buildVars = build.getEnvironment();

            String EIM_AGENTID_KEY = "ET_EIM_SUT_AGENT_ID";
            if (etBuildVars.containsKey(EIM_API_KEY) && etBuildVars.containsKey(EIM_AGENTID_KEY)
                    && (buildVars.containsKey(EIM_PACKETLOSS_KEY)
                            || buildVars.containsKey(EIM_CPUBURST_KEY))) {
                String eimApiUrl = etBuildVars.get(EIM_API_KEY);
                EIMManager eimManager = new EIMManager(eimApiUrl);

                String agentId = etBuildVars.get(EIM_AGENTID_KEY);

                // Packetloss
                if (buildVars.containsKey(EIM_PACKETLOSS_KEY)) {
                    String packetLossValue = buildVars.get(EIM_PACKETLOSS_KEY);
                    LOG.info("Removing packet loss {} to agent {} through EIM at {}",
                            packetLossValue, agentId, eimApiUrl);
                    eimManager.removePacketloss(agentId);
                }

                // Cpu burst
                if (buildVars.containsKey(EIM_CPUBURST_KEY)) {
                    String cpuBurstValue = buildVars.get(EIM_CPUBURST_KEY);
                    // LOG.info("Removing cpu burst {} to agent {} through EIM at {}",
                    // cpuBurstValue,
                    // eimApiUrl);
                    // TODO delete (in EIM side)
                }

            }
        } catch (Exception e) {
            LOG.warn("[elastest-plugin] EIM end manage: {}", e.getMessage());
        }
    }

}
