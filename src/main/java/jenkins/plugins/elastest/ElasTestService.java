package jenkins.plugins.elastest;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

import hudson.model.Run;

/**
 * 
 * @author Francisco R. Díaz
 *
 */
public class ElasTestService implements Serializable{
	private static final long serialVersionUID = 1;
	private static final Logger LOG = Logger.getLogger(ElasTestService.class.getName());
	private static ElasTestService instance;
	
	private HashMap<String, ExternalJob> externalJobs;
	private String elasTestApiUrl;
	private String elasTestUrl;
	private transient Client client;
	
	private ElasTestService(){
		this.externalJobs = new HashMap<>();
		elasTestUrl = ElasTestInstallation.getLogstashDescriptor().elasTestUrl;
		elasTestApiUrl = elasTestUrl + "/api/external/tjob";
		client = Client.create();
	}	

	/**
	 * 
	 * @param build
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws ClientHandlerException
	 * @throws UniformInterfaceException
	 * @throws IOException
	 */
	public ExternalJob asociateToElasTestTJob(Run<?, ?> build) throws Exception {		
		ExternalJob externalJob = new ExternalJob(build.getParent().getDisplayName());
		externalJob.settJobExecId(0L);		
		externalJob = createTJobOnElasTest(externalJob);
		externalJobs.put(build.getId(), externalJob);
		
		if (externalJob.getExecutionUrl().indexOf("http") < 0) {
			externalJob.setExecutionUrl(elasTestUrl + externalJob.getExecutionUrl());
			externalJob.setLogAnalyzerUrl(elasTestUrl + externalJob.getLogAnalyzerUrl());
		}
		
		return externalJob;
	}
	
	private ExternalJob createTJobOnElasTest(ExternalJob externalJob){
		ObjectMapper objetMapper = new ObjectMapper();
		WebResource webResource = client.resource(elasTestApiUrl);
		
		try {
			ClientResponse response = webResource.type("application/json").post(ClientResponse.class, externalJob.toJSON());
			externalJob = objetMapper.readValue(response.getEntity(String.class), ExternalJob.class);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
		return externalJob;
	}
	
	public void sendJobInformationToElasTest(ExternalJob externalJob) {
		LOG.info("Finalization message.");
		ObjectMapper objetMapper = new ObjectMapper();
		WebResource webResource = client.resource(elasTestApiUrl);		
		try {
			ClientResponse response = webResource.type("application/json").put(ClientResponse.class, externalJob.toJSON());			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();			
		}		
	}
	
	public ExternalJob getExternalJobByBuildId(String id){
		return externalJobs.get(id);
	}
	
	public HashMap<String, ExternalJob> getExternalJobs(){
		return externalJobs;
	}
	
	public void removeExternalJobs(String buildId){
		externalJobs.remove(buildId);
	}
		
	public static synchronized ElasTestService getInstance() {		
		if(instance == null){
			instance = new ElasTestService();
		}
		
		return instance;		
	}
}
