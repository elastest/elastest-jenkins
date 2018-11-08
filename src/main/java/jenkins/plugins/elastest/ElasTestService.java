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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;

import hudson.model.Run;
import jenkins.plugins.elastest.json.ElasTestBuild;
import jenkins.plugins.elastest.json.ExternalJob;
import jenkins.plugins.elastest.json.ExternalJob.ExternalJobStatusEnum;
import jenkins.plugins.elastest.json.Sut;
import jenkins.plugins.elastest.json.TestSupportServices;
import jenkins.plugins.elastest.pipeline.ElasTestStep;

/**
 * Service to communicate with ElasTest and store the info related to each TJob
 * execution by build.
 * 
 * @author Francisco R. Díaz
 * @since 0.0.1
 */
public class ElasTestService implements Serializable {
    private static final long serialVersionUID = 1;
    private static final Logger LOG = LoggerFactory
            .getLogger(ElasTestService.class);
    private static ElasTestService instance;

    private HashMap<String, ElasTestBuild> elasTestBuilds;
    private Map<String, String> tSServicesCatalog;
    private String elasTestTJobApiUrl;
    private String elasTestVersionApiUrl;
    private String elasTestUrl;
    private String credentialsB64;
    private transient Client client;

    public ElasTestService() {
        this.elasTestBuilds = new HashMap<>();
        elasTestUrl = ElasTestInstallation.getLogstashDescriptor().elasTestUrl;
        elasTestTJobApiUrl = elasTestUrl + "/api/external/tjob";
        elasTestVersionApiUrl = "/api/external/elastest/version";
        client = Client.create();
        client.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, new Integer(5000));
        client.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, new Integer(5000));
        //client.setConnectTimeout(5000);
        String name = ElasTestInstallation.getLogstashDescriptor().username;
        String password = ElasTestInstallation.getLogstashDescriptor().password;
        if ((name != null && !name.equals(""))
                && (password != null && !password.equals(""))) {
            String authString = name + ":" + password;
            credentialsB64 = new Base64().encodeAsString(
                    authString.getBytes(StandardCharsets.UTF_8));
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

    public void asociateToElasTestTJob(Run<?, ?> build,
            ElasTestBuildWrapper elasTestBuilder, ElasTestBuild elasTestBuild)
            throws Exception {
        LOG.info("Associate a Job to a TJob {}",
                build.getParent().getDisplayName());
        ExternalJob externalJob = new ExternalJob(
                build.getParent().getDisplayName());
        if (elasTestBuilder.isEus()) {
            List<String> tss = new ArrayList<>();
            tss.add("EUS");
            externalJob.setTSServices(prepareTSSToSendET(tss));
        }

        externalJob = asociateToElasTestTJob(build, externalJob);
        elasTestBuild.setExternalJob(externalJob);
        elasTestBuilds.put(build.getFullDisplayName(), elasTestBuild);

    }

    public void asociateToElasTestTJob(Run<?, ?> build,
            ElasTestStep elasTestStep, ElasTestBuild elasTestBuild)
            throws Exception {
        ExternalJob externalJob = new ExternalJob(
                build.getParent().getDisplayName());

        externalJob.setTSServices(prepareTSSToSendET(elasTestStep.getTss()));
        LOG.info("TestResutlPatter: "
                + elasTestStep.getSurefireReportsPattern());
        externalJob.setTestResultFilePattern(
                (elasTestStep.getSurefireReportsPattern() != null
                        && !elasTestStep.getSurefireReportsPattern().isEmpty())
                                ? elasTestStep.getSurefireReportsPattern()
                                : null);
        externalJob.setSut(
                elasTestStep.getSut() != -1L ? new Sut(elasTestStep.getSut())
                        : null);
        externalJob.setFromIntegratedJenkins(
                elasTestStep.envVars.get("INTEGRATED_JENKINS") != null
                        && elasTestStep.envVars.get("INTEGRATED_JENKINS")
                                .equals(Boolean.TRUE.toString())
                        && elasTestUrl.equals("http://etm:8091"));
        LOG.info("Build URL: {}", elasTestStep.envVars.get("BUILD_URL"));
        LOG.info("Job URL: {}", elasTestStep.envVars.get("JOB_URL"));
        externalJob.setBuildUrl(elasTestStep.envVars.get("BUILD_URL"));
        externalJob.setJobUrl(elasTestStep.envVars.get("JOB_URL"));
        externalJob.setProject(
                !elasTestStep.getProject().isEmpty() ? elasTestStep.getProject()
                        : null);
        externalJob = asociateToElasTestTJob(build, externalJob);
        elasTestBuild.setExternalJob(externalJob);
        elasTestBuilds.put(build.getFullDisplayName(), elasTestBuild);
    }

    public ExternalJob asociateToElasTestTJob(Run<?, ?> build,
            ExternalJob externalJob) throws Exception {
        externalJob.settJobExecId(0L);
        externalJob = createTJobOnElasTest(externalJob);
        externalJob.setExecutionUrl(externalJob.getExecutionUrl());
        externalJob.setLogAnalyzerUrl(externalJob.getLogAnalyzerUrl());

        LOG.info("Content of the external Job returned by ElasTest.");

        return externalJob;
    }

    private ExternalJob createTJobOnElasTest(ExternalJob externalJob)
            throws Exception {
        ObjectMapper objetMapper = new ObjectMapper();
        WebResource webResource = client.resource(elasTestTJobApiUrl);

        try {
            ClientResponse response = credentialsB64 != null
                    ? webResource.type("application/json")
                            .header("Authorization", "Basic " + credentialsB64)
                            .post(ClientResponse.class, externalJob.toJSON())
                    : webResource.type("application/json")
                            .post(ClientResponse.class, externalJob.toJSON());
            externalJob = objetMapper.readValue(
                    response.getEntity(String.class), ExternalJob.class);
            if (externalJob.getStatus() == ExternalJobStatusEnum.ERROR) {
                throw new Exception(externalJob.getError());
            }
        } catch (Exception e) {
            LOG.error("Error trying to create a TJob in ElasTest: {}",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return externalJob;
    }

    public ExternalJob isReadyTJobForExternalExecution(ExternalJob externalJob)
            throws Exception {
        ObjectMapper objetMapper = new ObjectMapper();
        WebResource webResource = client.resource(elasTestTJobApiUrl)
                .path(externalJob.gettJobExecId().toString());

        try {
            ClientResponse response = credentialsB64 != null
                    ? webResource.type("application/json")
                            .header("Authorization", "Basic " + credentialsB64)
                            .get(ClientResponse.class)
                    : webResource.type("application/json")
                            .get(ClientResponse.class);
            externalJob = objetMapper.readValue(
                    response.getEntity(String.class), ExternalJob.class);
            if (externalJob.getStatus() == ExternalJobStatusEnum.ERROR) {
                throw new Exception(externalJob.getError());
            }
        } catch (Exception e) {
            LOG.error("Error cheking if the TJob is ready: {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return externalJob;
    }

    public void finishElasTestTJobExecution(ExternalJob externalJob) {
        LOG.info("Finalization message.");
        WebResource webResource = client.resource(elasTestTJobApiUrl);
        try {
            if (credentialsB64 != null) {
                webResource.type("application/json")
                        .header("Authorization", "Basic " + credentialsB64)
                        .put(ClientResponse.class, externalJob.toJSON());
            } else {
                webResource.type("application/json").put(ClientResponse.class,
                        externalJob.toJSON());
            }

        } catch (Exception e) {
            LOG.error("Error sending the finalization message to ElasTest: {}",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public String getElasTestVersion() {
        String result = "KO";
        WebResource webResource = client.resource(elasTestUrl)
                .path(elasTestVersionApiUrl);

        try {
            ClientResponse response = credentialsB64 != null
                    ? webResource.type("text/plain")
                            .header("Authorization", "Basic " + credentialsB64)
                            .get(ClientResponse.class)
                    : webResource.type("text/plain").get(ClientResponse.class);

            result = response.getEntity(String.class);
            LOG.info("ElasTest version installed: " + result);
        } catch (UniformInterfaceException uie) {
            LOG.error("Error invoking ElasTest.");
            result = "The connection to ElasTest could not be established.";
            throw uie;
        } catch (Exception e) {
            LOG.error("Unknown error: {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }

        return result;
    }

    public ExternalJob getExternalJobByBuildFullName(String buildFullName) {
        return elasTestBuilds.get(buildFullName).getExternalJob();
    }

    public HashMap<String, ElasTestBuild> getElasTestBuild() {
        return elasTestBuilds;
    }

    public void removeExternalJobs(String buildId) {
        elasTestBuilds.remove(buildId);
    }

    public static synchronized ElasTestService getInstance() {
        if (instance == null) {
            instance = new ElasTestService();
        } else {
            instance.updateInstance();
        }

        return instance;
    }

    private List<TestSupportServices> prepareTSSToSendET(
            List<String> tSServices) {
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

    private void updateInstance() {
        LOG.info("Updating ElasTest service instance");
        instance.elasTestUrl = ElasTestInstallation
                .getLogstashDescriptor().elasTestUrl;
        instance.elasTestTJobApiUrl = elasTestUrl + "/api/external/tjob";
        instance.client = Client.create();
        String name = ElasTestInstallation.getLogstashDescriptor().username;
        String password = ElasTestInstallation.getLogstashDescriptor().password;
        if ((name != null && !name.equals(""))
                && (password != null && !password.equals(""))) {
            String authString = name + ":" + password;
            instance.credentialsB64 = new Base64().encodeAsString(
                    authString.getBytes(StandardCharsets.UTF_8));
            LOG.info("Now access to ElasTest is with username and password.");
        } else {
            instance.credentialsB64 = null;
            LOG.info(
                    "Now access to ElasTest is without username and password.");
        }

        instance.tSServicesCatalog = loadTSSCatalog();
    }
}
