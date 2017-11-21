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
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

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
import jenkins.tasks.SimpleBuildWrapper;

/**
 * Build wrapper that decorates the build's logger to send to ElasTest
 * 
 * @author Francisco R. Díaz
 */
public class ElasTestBuildWrapper extends SimpleBuildWrapper {
    private static final Logger LOG = Logger
            .getLogger(ElasTestBuildWrapper.class.getName());

    /**
     * Create a new {@link ElasTestBuildWrapper}.
     */
    @DataBoundConstructor
    public ElasTestBuildWrapper() {
        super();
        LOG.info("ElasTestBuildWrapper Constructor");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace,
            Launcher launcher, TaskListener listener,
            EnvVars initialEnvironment)
            throws IOException, InterruptedException {
        // nothing to do
        LOG.info("ElasTestBuildWrapper SetUp");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConsoleLogFilter createLoggerDecorator(Run<?, ?> build) {
        LOG.info("ElasTestBuildWrapper CreateLoggerDecorator");
        ElasTestService elasTestService = ElasTestService.getInstance();
        try {
            elasTestService.asociateToElasTestTJob(build);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ConsoleLogFilterImpl(build, elasTestService);
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
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
