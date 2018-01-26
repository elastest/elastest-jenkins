package jenkins.plugins.elastest;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Environment;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Run.RunnerAbortedException;
import hudson.model.listeners.RunListener;
import jenkins.plugins.elastest.json.ExternalJob;

/**
 * Listener
 * 
 * @author frdiaz
 *
 */
@Extension
public class BuildListener extends RunListener<Run> {
    private static final Logger LOG = LoggerFactory
            .getLogger(BuildListener.class);

    private String elasTestApiURL;
    // @Inject
    private ElasTestService elasTestService;

    public BuildListener() {
        LOG.info("Initializing Listener");
        elasTestApiURL = ElasTestInstallation
                .getLogstashDescriptor().elasTestUrl + "/api/external/tjob";
        elasTestService = ElasTestService.getInstance();
    }

    @Override
    public void onStarted(Run r, TaskListener listener) {
        LOG.info("Listener on started");
        super.onStarted(r, listener);
    }

    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher,
            hudson.model.BuildListener listener)
            throws IOException, InterruptedException, RunnerAbortedException {
        LOG.info("Set up environment");
        
        return super.setUpEnvironment(build, launcher, listener);
    }

    @Override
    public void onCompleted(Run run, TaskListener listener) {
        LOG.info("Resultado:  " + run.getResult().ordinal);
    }

    @Override
    public void onFinalized(Run build) {
        super.onFinalized(build);

        if (elasTestService.getExternalJobs().size() > 0) {
            ExternalJob externalJob = elasTestService
                    .getExternalJobByBuildId(build.getId());
            switch (build.getResult().ordinal) {
            case 0:
                externalJob.setResult(0);
                break;
            case 1:
            case 2:
            case 3:
                externalJob.setResult(1);
                break;
            case 4:
                externalJob.setResult(3);
                break;
            default:
                externalJob.setResult(0);
                break;
            }

            elasTestService.sendJobInformationToElasTest(
                    elasTestService.getExternalJobByBuildId(build.getId()));
            elasTestService.removeExternalJobs(build.getId());
            LOG.info("Resultado:  " + build.getResult().ordinal);
        }

        LOG.info("Finalized all");
    }
}
