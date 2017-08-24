package jenkins.plugins.elastest;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Environment;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Run.RunnerAbortedException;
import hudson.model.listeners.RunListener;
import jenkins.plugins.elastest.action.ElasTestItemMenuAction;
import jenkins.plugins.elastest.submiter.BuildData;

/**
 * Listener 
 * @author frdiaz
 *
 */
@Extension
public class BuildListener extends RunListener<Run> {
	private static final Logger LOG = Logger.getLogger(BuildListener.class.getName());
	
	private String elasTestApiURL;
	private ElasTestService elasTestService;

	public BuildListener() {
		LOG.info("Initializing Listener");
		elasTestApiURL = ElasTestInstallation.getLogstashDescriptor().elasTestUrl + "/api/external/tjob";
		elasTestService = ElasTestService.getInstance();
	}

	@Override
	public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, hudson.model.BuildListener listener)
			throws IOException, InterruptedException, RunnerAbortedException {
		LOG.info("Set up environment");		
		ElasTestItemMenuAction action = new ElasTestItemMenuAction(build, null);
		build.addAction(action);		
		action.setElasTestLogAnalyzerUrl(elasTestService.getExternalJobByBuildId(build.getId()).getLogAnalyzerUrl());
		return super.setUpEnvironment(build, launcher, listener);
	}

	@Override
	public void onCompleted(Run run, TaskListener listener) {
	}

	@Override
	public void onFinalized(Run build) {
		super.onFinalized(build);				

		if (elasTestService.getExternalJobs().size() > 0) {
			elasTestService.sendJobInformationToElasTest(elasTestService.getExternalJobByBuildId(build.getId()));
			elasTestService.removeExternalJobs(build.getId());
		}
		
		LOG.info("Finalized");
	}

	
	
}
