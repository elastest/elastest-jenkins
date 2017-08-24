/*
 * The MIT License
 * 
 * Copyright (c) 2017 Steven G. Brown, ElasTest
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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.BodyInvoker;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.EnvVars;
import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import jenkins.YesNoMaybe;
import jenkins.plugins.elastest.BuildListener;
import jenkins.plugins.elastest.ConsoleLogFilterImpl;
import jenkins.plugins.elastest.ElasTestService;
import jenkins.plugins.elastest.Messages;
import jenkins.plugins.elastest.action.ElasTestItemMenuAction;

/**
 * Pipeline plug-in step for sending log traces to ElasTest Platform.
 * 
 * @author Steven G. Brown
 * @author Francisco R. Díaz
 */
public class ElasTestStep extends AbstractStepImpl {

	private static final Logger LOG = Logger.getLogger(ElasTestStep.class.getName());
		
	/**
	 * Constructor.
	 */
	@DataBoundConstructor
	public ElasTestStep() {		
	}

	/**
	 * Execution for {@link ElasTestStep}.
	 */
	public static class ExecutionImpl extends AbstractStepExecutionImpl {

		private static final long serialVersionUID = 1L;
		
		private ElasTestService elasTestService;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean start() throws Exception {
			LOG.info("Step start.");
			elasTestService = ElasTestService.getInstance();			
			StepContext context = getContext();
			Run<?, ?> build = context.get(Run.class);
			try {
				elasTestService.asociateToElasTestTJob(build);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			context.newBodyInvoker().withContext(createConsoleLogFilter(context, build))
					.withCallback(BodyExecutionCallback.wrap(context)).start();
			return false;
		}

		private ConsoleLogFilter createConsoleLogFilter(StepContext context, Run<?, ?> build)
				throws IOException, InterruptedException {
			ConsoleLogFilterImpl logFilterImpl =  new ConsoleLogFilterImpl(build, elasTestService);			
			ElasTestItemMenuAction action = new ElasTestItemMenuAction(build, null);
			build.addAction(action);			
			action.setElasTestLogAnalyzerUrl(elasTestService.getExternalJobByBuildId(build.getId()).getLogAnalyzerUrl());		
			
			return logFilterImpl;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void stop(@Nonnull Throwable cause) throws Exception {
			getContext().onFailure(cause);
		}
	}

	/**
	 * Descriptor for {@link ElasTestStep}.
	 */
	@Extension(dynamicLoadable = YesNoMaybe.YES, optional = true)
	public static class DescriptorImpl extends AbstractStepDescriptorImpl {

		/**
		 * Constructor.
		 */
		public DescriptorImpl() {
			super(ExecutionImpl.class);
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
		public String getFunctionName() {
			return Messages.ElasTestPipelineStep();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean takesImplicitBlockArgument() {
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getHelpFile() {
			return getDescriptorFullUrl() + "/help";
		}

		/**
		 * Serve the help file.
		 * 
		 * @param request
		 * @param response
		 * @throws IOException
		 */
		public void doHelp(StaplerRequest request, StaplerResponse response) throws IOException {
			response.setContentType("text/html;charset=UTF-8");
			PrintWriter writer = response.getWriter();
			writer.println(Messages.DisplayName());
			writer.flush();
		}
	}
}
