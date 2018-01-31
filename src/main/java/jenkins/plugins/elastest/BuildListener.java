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
 * Listener for the Job life cycle.
 * 
 * @author Francisco R Diaz
 * @since 0.0.1
 */
@Extension
public class BuildListener extends RunListener<Run> {
    private static final Logger LOG = LoggerFactory
            .getLogger(BuildListener.class);

    private String elasTestApiURL;
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

//    @Override
//    public void onCompleted(Run run, TaskListener listener) {
//        LOG.info("Resultado:  " + (run != null ? run.getResult().ordinal : "Not available."));
//    }

    @Override
    public void onFinalized(Run build) {
        super.onFinalized(build);

        if (elasTestService.getExternalJobs().size() > 0
                && (build != null && build.getId() != null && build.getResult() != null)) {
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
