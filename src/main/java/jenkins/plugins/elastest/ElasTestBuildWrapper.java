/*
 * The MIT License
 *
 * Copyright 2013 Hewlett-Packard Development Company, L.P.
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
import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.plugins.elastest.action.ElasTestItemMenuAction;
import jenkins.plugins.elastest.json.ExternalJob;
import jenkins.tasks.SimpleBuildWrapper;

/**
 * Build wrapper that decorates the build's logger to send to ElasTest and allow you to use
 * the EUS(ElasTest User Impersonation Service) from a Jenkins Job.
 * 
 * @author Francisco R. Díaz
 * @since 0.0.1
 */
public class ElasTestBuildWrapper extends SimpleBuildWrapper {
    private static final Logger LOG = Logger
            .getLogger(ElasTestBuildWrapper.class.getName());

    private ElasTestService elasTestService;
    
    private boolean eus;

    /**
     * Create a new {@link ElasTestBuildWrapper}.
     */
    @DataBoundConstructor
    public ElasTestBuildWrapper() {
        super();
        LOG.info("ElasTestBuildWrapper Constructor");
    }
    
    public boolean isEus() {
        return eus;
    }

    @DataBoundSetter
    public void setEus(boolean eus) {
        this.eus = eus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace,
            Launcher launcher, TaskListener listener,
            EnvVars initialEnvironment)
            throws IOException, InterruptedException {
        LOG.info("ElasTestBuildWrapper SetUp");
        ElasTestItemMenuAction.addActionToMenu(build);
        ExternalJob externalJob = elasTestService
                .getExternalJobByBuildId(build.getId());
        while (!externalJob.isReady()) {
            try {
                externalJob = elasTestService
                        .isReadyTJobForExternalExecution(externalJob);
                elasTestService.getExternalJobs().put(build.getId(),
                        externalJob);
            } catch (Exception e) {
                LOG.info("Error checking the status of the TJob.");
                e.printStackTrace();
                throw new InterruptedException();
            }
        }

        if (elasTestService.getExternalJobByBuildId(build.getId())
                .getTSSEnvVars() != null) {
            for (Entry<String, String> entry : elasTestService
                    .getExternalJobByBuildId(build.getId()).getTSSEnvVars()
                    .entrySet()) {
                context.env(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConsoleLogFilter createLoggerDecorator(Run<?, ?> build) {
        LOG.info("ElasTestBuildWrapper CreateLoggerDecorator");
        elasTestService = ElasTestService.getInstance();
        try {
            elasTestService.asociateToElasTestTJob(build, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ConsoleLogFilterImpl(build, elasTestService);
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }
    
    // Method to encapsulate calls for unit-testing
    ElasTestWriter getElasTestWriter(Run<?, ?> build, OutputStream errorStream, ExternalJob externalJob) {
      return new ElasTestWriter(build, errorStream, null, externalJob);
    }

    /**
     * Registers {@link ElasTestBuildWrapper} as a {@link BuildWrapper}.
     */
    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        public DescriptorImpl() {
            super(ElasTestBuildWrapper.class);
            load();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }
    }
}
