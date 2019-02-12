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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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
import hudson.remoting.VirtualChannel;
import jenkins.plugins.elastest.docker.DockerCommandExecutor;
import jenkins.plugins.elastest.docker.DockerService;
import jenkins.plugins.elastest.json.ElasTestBuild;
import jenkins.plugins.elastest.json.ExternalJob;
import jenkins.plugins.elastest.utils.ParseResultCallable;

/**
 * Listener to perform some actions during the Job life cycle.
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
    private DockerService dockerService;
    private DockerCommandExecutor dockerCommandExecutor;

    public BuildListener() {
        LOG.debug("[elastest-plugin]: Initializing Listener");
        elasTestApiURL = ElasTestInstallation
                .getLogstashDescriptor().elasTestUrl + "/api/external/tjob";
        elasTestService = ElasTestService.getInstance();
        dockerService = DockerService
                .getDockerService(DockerService.DOCKER_HOST_BY_DEFAULT);
        dockerCommandExecutor = new DockerCommandExecutor(null,
                dockerService);

    }

    @Override
    public void onStarted(Run r, TaskListener listener) {
        LOG.debug("[elastest-plugin]: Listener on started");
        super.onStarted(r, listener);
    }

    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher,
            hudson.model.BuildListener listener)
            throws IOException, InterruptedException, RunnerAbortedException {
        LOG.debug(" [elastest-plugin]:Set up environment");

        return super.setUpEnvironment(build, launcher, listener);
    }

    @Override
    public void onCompleted(Run run, TaskListener listener) {

        final long buildTime = run.getTimestamp().getTimeInMillis();
        final long timeOnMaster = System.currentTimeMillis();

        ElasTestBuild elasTestBuild = elasTestService.getElasTestBuilds()
                .get(run.getFullDisplayName());

        if (elasTestBuild.getExternalJob().getTestResultFilePattern() != null
                && !elasTestBuild.getExternalJob().getTestResultFilePattern()
                        .isEmpty()) {
            try {
                elasTestBuild.getExternalJob()
                        .setTestResults(elasTestBuild.getWorkspace()
                                .act(new ParseResultCallable(
                                        elasTestBuild.getExternalJob()
                                                .getTestResultFilePattern(),
                                        buildTime, timeOnMaster)));
            } catch (IOException | InterruptedException e) {
                listener.getLogger().println("Error sending surefire reports");
            }
        }

    }

    @Override
    public void onFinalized(Run build) {
        super.onFinalized(build);

        if (elasTestService.getElasTestBuilds().size() > 0
                && (build != null && build.getFullDisplayName() != null
                        && build.getResult() != null)) {
            ExternalJob externalJob = elasTestService
                    .getExternalJobByBuildFullName(build.getFullDisplayName());
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
            // Stop docker containers started locally
            LOG.debug("[elastest-plugin]: Stopping aux containers.");
            try {
                ElasTestBuild elasTestBuild = elasTestService
                .getElasTestBuilds().get(build.getFullDisplayName());
                List<String> buildContainers = elasTestService
                        .getElasTestBuilds().get(build.getFullDisplayName())
                        .getContainers();
                if (buildContainers.size() > 0) {
                    VirtualChannel channel = elasTestBuild.getWorkspace().getChannel();
                    dockerCommandExecutor.setCommand("docker", "ps");
                    channel.call(dockerCommandExecutor);
                    for (String containerId : elasTestService
                            .getElasTestBuilds().get(build.getFullDisplayName())
                            .getContainers()) {
                        LOG.info("Stopping docker container: {}", containerId);
                        dockerCommandExecutor.setCommand("docker", "rm", "-f",
                        containerId, "");
                        channel.call(dockerCommandExecutor);
                    }
                }
            } catch (RuntimeException | IOException | InterruptedException io) {
                LOG.warn(
                        "[elastest-plugin]: Error stopping monitoring containers. It's possible "
                                + "that you will have to stop them manually");
                io.printStackTrace();
            } finally {
                elasTestService.finishElasTestTJobExecution(
                        elasTestService.getExternalJobByBuildFullName(
                                build.getFullDisplayName()));
                ExecutorService executor = elasTestService.getElasTestBuilds()
                        .get(build.getFullDisplayName()).getWriter()
                        .getExecutor();
                if (elasTestService.getElasTestBuilds()
                        .get(build.getFullDisplayName()) != null
                        && elasTestService.getElasTestBuilds()
                                .get(build.getFullDisplayName())
                                .getWriter() != null
                        && !executor.isTerminated()) {
                    executor.shutdown();
                    try {
                        executor.awaitTermination(60, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        LOG.warn("Timeout sending logs to ElasTest");
                    }
                }
                elasTestService.removeExternalJobs(build.getFullDisplayName());
            }
        }
        LOG.info("[elastest-plugin]: Finalized all");
    }
}
