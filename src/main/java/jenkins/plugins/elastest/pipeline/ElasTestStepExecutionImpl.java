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
package jenkins.plugins.elastest.pipeline;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.EnvVars;
import hudson.console.ConsoleLogFilter;
import hudson.model.Run;
import jenkins.plugins.elastest.ConsoleLogFilterImpl;
import jenkins.plugins.elastest.ElasTestService;
import jenkins.plugins.elastest.action.ElasTestItemMenuAction;
import jenkins.plugins.elastest.docker.DockerService;
import jenkins.plugins.elastest.json.ElasTestBuild;
import jenkins.plugins.elastest.json.ExternalJob;

/**
 * Execution for {@link ElasTestStep}.
 * 
 * @author Francisco R. Díaz
 * @since 0.0.1
 */
public class ElasTestStepExecutionImpl extends AbstractStepExecutionImpl {

    private static final Logger LOG = LoggerFactory
            .getLogger(ElasTestStepExecutionImpl.class);
    private static final long serialVersionUID = 1L;
    private static final String ETM_CONTAINER_NAME = "elastest_etm_1";

    private ElasTestService elasTestService;
    private DockerService dockerService;

    @Inject
    transient ElasTestStep elasTestStep;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean start() throws Exception {
        elasTestService = ElasTestService.getInstance();
        StepContext context = getContext();
        Run<?, ?> build = context.get(Run.class);
        ElasTestBuild elasTestBuild = null;
        try {
            // Init Build Context
            elasTestBuild = new ElasTestBuild(context);
            // Associate the Jenkins' Job to an ElasTest Job
            elasTestService.asociateToElasTestTJob(build, elasTestStep,
                    elasTestBuild);
            // Add the ElasTest menu item to the left menu
            ElasTestItemMenuAction.addActionToMenu(build);
            // Wait until the ElasTest Job is ready
            while (!elasTestBuild.getExternalJob().isReady()) {
                elasTestBuild.setExternalJob(
                        elasTestService.isReadyTJobForExternalExecution(
                                elasTestBuild.getExternalJob()));
            }
            // Set environment variables
            addEnvVars(build);
            // If monitoring is true, start monitoring
            if (elasTestStep.isMonitoring()) {
                startMonitoringContainers(elasTestStep.envVars, elasTestBuild);
            }
        } catch (Exception e) {
            LOG.error("Error trying to bind the build with a TJob.");
            e.printStackTrace();
            throw e;
        }

        ExpanderImpl expanderImpl = new ExpanderImpl();
        expanderImpl.setOverrides(elasTestStep.envVars);
        expanderImpl.expand(getContext().get(EnvVars.class));

        context.newBodyInvoker()
                .withContext(createConsoleLogFilter(context, build))
                .withContext(EnvironmentExpander.merge(
                        getContext().get(EnvironmentExpander.class),
                        expanderImpl))
                .withCallback(BodyExecutionCallback.wrap(getContext())).start();
        return false;
    }

    private static final class ExpanderImpl extends EnvironmentExpander {
        private static final long serialVersionUID = 1;
        private Map<String, String> overrides = new HashMap<>();

        @Override
        public void expand(EnvVars env)
                throws IOException, InterruptedException {
            env.overrideAll(overrides);
        }

        public void setOverrides(Map<String, String> overrides) {
            this.overrides = overrides;
        }
    }

    private ConsoleLogFilter createConsoleLogFilter(StepContext context,
            Run<?, ?> build) throws IOException, InterruptedException {
        LOG.info("Creatin console log filter.");
        ConsoleLogFilterImpl logFilterImpl = new ConsoleLogFilterImpl(build,
                elasTestService);
        return logFilterImpl;
    }

    private void addEnvVars(Run<?, ?> build) {
        ExternalJob externalJob = elasTestService
                .getExternalJobByBuildFullName(build.getFullDisplayName());
        elasTestStep.envVars.putAll(
                externalJob.getEnvVars() != null ? externalJob.getEnvVars()
                        : new HashMap<String, String>());
    }

    private void startMonitoringContainers(EnvVars envVars,
            ElasTestBuild elasTestBuild) {
        LOG.info("Start container monitoring");
        dockerService = DockerService
                .getDockerService(DockerService.DOCKER_HOST_BY_DEFAULT);

        String fileBeatImage = "elastest/etm-filebeat:latest";
        String dockBeatImage = "elastest/etm-dockbeat:latest";

        String logstashHost = "LOGSTASHHOST="
                + (!envVars.get("ET_MON_LSBEATS_HOST").trim().equals("localhost")
                        ? envVars.get("ET_MON_LSBEATS_HOST")
                        : dockerService
                                .getGatewayFromContainer(ETM_CONTAINER_NAME));
        String logstashPort = "LOGSTASHPORT="
                + envVars.get("ET_MON_INTERNAL_LSBEATS_PORT");
        String etMonLsbeatsHost = "ET_MON_LSBEATS_HOST="
                + envVars.get("ET_MON_LSBEATS_HOST");
        String etMonLsbeatsPort = "ET_MON_LSBEATS_PORT="
                + envVars.get("ET_MON_LSBEATS_PORT");
        String etMonContainersName = "ET_MON_CONTAINERS_NAME=" + "^("
                + envVars.get("ET_SUT_CONTAINER_NAME") + ")(_)?(\\d*)(.*)?";

        if (isRemoteElasTest()) {
            elasTestBuild.getContainers()
                    .add(dockerService.executeDockerCommand("docker", "run",
                            "-d", "-e", etMonLsbeatsHost, "-e",
                            etMonLsbeatsPort, "-e", etMonContainersName, "-v",
                            "/var/run/docker.sock:/var/run/docker.sock", "-v",
                            "/var/lib/docker/containers:/var/lib/docker/containers",
                            "--network=elastest_elastest", fileBeatImage));
        }

        elasTestBuild.getContainers()
                .add(dockerService.executeDockerCommand("docker", "run", "-d",
                        "-e", logstashHost, "-e", logstashPort, "-v",
                        "/var/run/docker.sock:/var/run/docker.sock", "-v",
                        "/var/lib/docker/containers:/var/lib/docker/containers",
                        "--network=elastest_elastest", dockBeatImage));
    }

    private boolean isRemoteElasTest() {
        LOG.info("Checking if ElasTest is running locally.");
        boolean result = true;
        String etContainername = "elastest_etm_1";
        String errorMessage = "No such object: " + etContainername;
        result = dockerService
                .executeDockerCommand("docker", "inspect",
                        "--format=\\\"{{.Name}}\\\"", etContainername)
                .contains(errorMessage);
        LOG.info("Result of the inspect command: {}", result);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {
        getContext().onFailure(cause);
    }

}
