package jenkins.plugins.elastest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper;
import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsConfig;
import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper.VarPasswordPair;

import hudson.console.ConsoleLogFilter;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;
import jenkins.plugins.elastest.json.ExternalJob;

public class ConsoleLogFilterImpl extends ConsoleLogFilter implements Serializable {
	private static final long serialVersionUID = 1;
	private static final Logger LOG = Logger.getLogger(ConsoleLogFilterImpl.class.getName());
	
	private final transient Run<?, ?> build;	
	private ElasTestService elasTestService;

	public ConsoleLogFilterImpl(Run<?, ?> build, ElasTestService elasTestService) {		
		this.build = build;
		this.elasTestService = elasTestService;	
	}

	@SuppressWarnings("rawtypes")
	@Override
	public OutputStream decorateLogger(Run _ignore, OutputStream logger)
			throws IOException, InterruptedException {
						
		ElasTestWriter logstash = getLogStashWriter(build, logger, elasTestService.getExternalJobByBuildId(build.getId()));
		ElasTestOutputStream los = new ElasTestOutputStream(logger, logstash);		

		if (build.getParent() instanceof BuildableItemWithBuildWrappers) {
			BuildableItemWithBuildWrappers project = (BuildableItemWithBuildWrappers) build.getParent();
			for (BuildWrapper wrapper : project.getBuildWrappersList()) {
				if (wrapper instanceof MaskPasswordsBuildWrapper) {
					List<VarPasswordPair> allPasswordPairs = new ArrayList<VarPasswordPair>();

					MaskPasswordsBuildWrapper maskPasswordsWrapper = (MaskPasswordsBuildWrapper) wrapper;
					List<VarPasswordPair> jobPasswordPairs = maskPasswordsWrapper.getVarPasswordPairs();
					if (jobPasswordPairs != null) {
						allPasswordPairs.addAll(jobPasswordPairs);
					}

					MaskPasswordsConfig config = MaskPasswordsConfig.getInstance();
					List<VarPasswordPair> globalPasswordPairs = config.getGlobalVarPasswordPairs();
					if (globalPasswordPairs != null) {
						allPasswordPairs.addAll(globalPasswordPairs);
					}

					return los.maskPasswords(allPasswordPairs);
				}
			}
		}

		return los;
	}
	

	// Method to encapsulate calls for unit-testing
	ElasTestWriter getLogStashWriter(Run<?, ?> build, OutputStream errorStream, ExternalJob externalJob) {
		return new ElasTestWriter(build, errorStream, null, externalJob);
	}
}