package jenkins.plugins.elastest;

import java.io.Serializable;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import hudson.model.Run;

/**
 * 
 * @author Francisco R. Díaz
 *
 */
public class ElasTestService implements Serializable {
    private static final long serialVersionUID = 1;
    private static final Logger log = LoggerFactory
            .getLogger(ElasTestService.class);
    private static ElasTestService instance;

    private HashMap<String, ExternalJob> externalJobs;
    private String elasTestApiUrl;
    private String elasTestUrl;
    private transient Client client;

    private ElasTestService() {
        this.externalJobs = new HashMap<>();
        elasTestUrl = ElasTestInstallation.getLogstashDescriptor().elasTestUrl;
        elasTestApiUrl = elasTestUrl + "/api/external/tjob";
        client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter(
                ElasTestInstallation.getLogstashDescriptor().username,
                ElasTestInstallation.getLogstashDescriptor().password));
    }

    public ExternalJob asociateToElasTestTJob(Run<?, ?> build)
            throws Exception {
        ExternalJob externalJob = new ExternalJob(
                build.getParent().getDisplayName());
        externalJob.settJobExecId(0L);
        externalJob = createTJobOnElasTest(externalJob);
        externalJobs.put(build.getId(), externalJob);
        externalJob.setExecutionUrl(externalJob.getExecutionUrl());
        externalJob.setLogAnalyzerUrl(externalJob.getLogAnalyzerUrl());
        return externalJob;
    }

    private ExternalJob createTJobOnElasTest(ExternalJob externalJob)
            throws Exception {
        ObjectMapper objetMapper = new ObjectMapper();
        WebResource webResource = client.resource(elasTestApiUrl);

        try {
            ClientResponse response = webResource.type("application/json")
                    .post(ClientResponse.class, externalJob.toJSON());
            externalJob = objetMapper.readValue(
                    response.getEntity(String.class), ExternalJob.class);
        } catch (Exception e) {
            log.error("Error in the creation of a TJob in ElasTest: {}",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return externalJob;
    }

    public void sendJobInformationToElasTest(ExternalJob externalJob) {
        log.info("Finalization message.");
        ObjectMapper objetMapper = new ObjectMapper();
        WebResource webResource = client.resource(elasTestApiUrl);
        try {
            ClientResponse response = webResource.type("application/json")
                    .put(ClientResponse.class, externalJob.toJSON());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public ExternalJob getExternalJobByBuildId(String id) {
        return externalJobs.get(id);
    }

    public HashMap<String, ExternalJob> getExternalJobs() {
        return externalJobs;
    }

    public void removeExternalJobs(String buildId) {
        externalJobs.remove(buildId);
    }

    public static synchronized ElasTestService getInstance() {
        if (instance == null) {
            instance = new ElasTestService();
        }

        return instance;
    }
}
