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
import jenkins.plugins.elastest.json.ExternalJob;

/**
 * Execution for {@link ElasTestStep}.
 */
public class ExecutionImpl extends AbstractStepExecutionImpl {

    private static final Logger logger = LoggerFactory
            .getLogger(ExecutionImpl.class);
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
        try {
            elasTestService.asociateToElasTestTJob(build, elasTestStep);
            addEnvVars(build);
        } catch (Exception e) {
            logger.error("Error trying to bind the build with a TJob.");
            e.printStackTrace();
            throw e;
        }

        ExpanderImpl expanderImpl = new ExpanderImpl();
        expanderImpl.setOverrides(elasTestStep.envVars);
        expanderImpl.expand(getContext().get(EnvVars.class));
        
        context.newBodyInvoker()
                .withContext(createConsoleLogFilter(context, build))
                .withContext(EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class), expanderImpl))
                .withCallback(BodyExecutionCallback.wrap(getContext())).start();
        return false;
    }
    
    private static final class ExpanderImpl extends EnvironmentExpander {
        private static final long serialVersionUID = 1;
        private Map<String,String> overrides = new HashMap<>();
        
        @Override
        public void expand(EnvVars env)
                throws IOException, InterruptedException {
            env.overrideAll(overrides);
        }
        
        public void setOverrides(Map<String, String> overrides){
            this.overrides = overrides;
        }
    }

    private ConsoleLogFilter createConsoleLogFilter(StepContext context,
            Run<?, ?> build) throws IOException, InterruptedException {
        logger.info("Creatin console log filter.");
        ConsoleLogFilterImpl logFilterImpl = new ConsoleLogFilterImpl(build,
                elasTestService);
        ElasTestItemMenuAction action = new ElasTestItemMenuAction(build,
                elasTestService.getExternalJobByBuildId(build.getId())
                        .getLogAnalyzerUrl(),
                elasTestService.getExternalJobByBuildId(build.getId())
                        .getExecutionUrl());
        build.addAction(action);

        return logFilterImpl;
    }

    private void addEnvVars(Run<?, ?> build) {
        ExternalJob externalJob = elasTestService
                .getExternalJobByBuildId(build.getId());
        elasTestStep.envVars.putAll(externalJob.getTSSEnvVars());
        for (Map.Entry<String, String> entry : elasTestStep.envVars
                .entrySet()) {
            logger.info("Environment variable => key: {}, value: {}",
                    entry.getKey(), entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {
        getContext().onFailure(cause);
    }
}
