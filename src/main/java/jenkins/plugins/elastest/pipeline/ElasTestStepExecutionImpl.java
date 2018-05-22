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
import hudson.FilePath;
import hudson.console.ConsoleLogFilter;
import hudson.model.Run;
import jenkins.plugins.elastest.ConsoleLogFilterImpl;
import jenkins.plugins.elastest.ElasTestService;
import jenkins.plugins.elastest.action.ElasTestItemMenuAction;
import jenkins.plugins.elastest.json.ElasTestBuild;
import jenkins.plugins.elastest.json.ExternalJob;

/**
 * Execution for {@link ElasTestStep}.
 
 * @author Francisco R. Díaz
 * @since 0.0.1
 */
public class ElasTestStepExecutionImpl extends AbstractStepExecutionImpl {

    private static final Logger LOG = LoggerFactory
            .getLogger(ElasTestStepExecutionImpl.class);
    private static final long serialVersionUID = 1L;

    private ElasTestService elasTestService;
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
            elasTestBuild = new ElasTestBuild(context);
            elasTestService.asociateToElasTestTJob(build, elasTestStep, elasTestBuild);            
                        
            ElasTestItemMenuAction.addActionToMenu(build);
            while (!elasTestBuild.getExternalJob().isReady()) {
                elasTestBuild.setExternalJob(elasTestService
                        .isReadyTJobForExternalExecution(elasTestBuild.getExternalJob()));                
            }
            
            addEnvVars(build);
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
        elasTestStep.envVars.putAll(externalJob.getTSSEnvVars() != null
                ? externalJob.getTSSEnvVars() : new HashMap<String, String>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {
        getContext().onFailure(cause);
    }
    
    
}
