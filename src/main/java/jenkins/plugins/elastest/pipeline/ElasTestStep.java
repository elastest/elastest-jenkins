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
import java.io.PrintWriter;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.Run;
import jenkins.YesNoMaybe;
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

	private static final Logger logger = LoggerFactory.getLogger(ElasTestStep.class);
		
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
			logger.info("Step start.");
			elasTestService = ElasTestService.getInstance();			
			StepContext context = getContext();
			Run<?, ?> build = context.get(Run.class);
			try {
				elasTestService.asociateToElasTestTJob(build);
			} catch (Exception e) {
			    logger.error("Error trying to bind the build with a TJob.");
				e.printStackTrace();
				throw e;
			}
			
			
			context.newBodyInvoker().withContext(createConsoleLogFilter(context, build))
					.withCallback(BodyExecutionCallback.wrap(context)).start();
			return false;
		}

		private ConsoleLogFilter createConsoleLogFilter(StepContext context, Run<?, ?> build)
				throws IOException, InterruptedException {
		    logger.info("Creatin console log filter.");
		    ConsoleLogFilterImpl logFilterImpl =  new ConsoleLogFilterImpl(build, elasTestService);			
			ElasTestItemMenuAction action = new ElasTestItemMenuAction(build, elasTestService.getExternalJobByBuildId(build.getId()).getLogAnalyzerUrl(),
			        elasTestService.getExternalJobByBuildId(build.getId()).getExecutionUrl());
			build.addAction(action);
			
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
