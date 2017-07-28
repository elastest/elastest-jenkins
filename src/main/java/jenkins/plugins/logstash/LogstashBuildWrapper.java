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

package jenkins.plugins.logstash;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.plugins.elastest.ExternalJob;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper;
import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper.VarPasswordPair;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsConfig;

/**
 * Build wrapper that decorates the build's logger to insert a
 * {@link LogstashNote} on each output line.
 *
 * @author K Jonathan Harker
 */
public class LogstashBuildWrapper extends BuildWrapper {

  /**
   * Create a new {@link LogstashBuildWrapper}.
   */
  @DataBoundConstructor
  public LogstashBuildWrapper() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

    return new Environment() {};
  }

  /**
   * {@inheritDoc}
 * @throws IOException 
 * @throws JsonMappingException 
 * @throws JsonParseException 
   */
  @Override
  public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) throws JsonParseException, JsonMappingException, IOException {
	  System.out.println("Execution order: 1");
	  ExternalJob externalJob = asociateToElasTestTJob(build, logger);
	  LogstashWriter logstash = getLogStashWriter(build, logger, externalJob);   
    

    LogstashOutputStream los = new LogstashOutputStream(logger, logstash);

    if (build.getProject() instanceof BuildableItemWithBuildWrappers) {
      BuildableItemWithBuildWrappers project = (BuildableItemWithBuildWrappers) build.getProject();
      for (BuildWrapper wrapper: project.getBuildWrappersList()) {
        if (wrapper instanceof MaskPasswordsBuildWrapper) {
          List<VarPasswordPair> allPasswordPairs = new ArrayList<VarPasswordPair>();

          MaskPasswordsBuildWrapper maskPasswordsWrapper = (MaskPasswordsBuildWrapper) wrapper;
          List<VarPasswordPair> jobPasswordPairs = maskPasswordsWrapper.getVarPasswordPairs();
          if (jobPasswordPairs != null) {
            allPasswordPairs.addAll(jobPasswordPairs);
          }

          MaskPasswordsConfig config = MaskPasswordsConfig.getInstance();
          List<VarPasswordPair> globalPasswordPairs = config.getGlobalVarPasswordPairs();
          if (globalPasswordPairs != null) {
            allPasswordPairs.addAll(globalPasswordPairs);
          }

          return los.maskPasswords(allPasswordPairs);
        }
      }
    }

    return los;
  }
  
  private ExternalJob asociateToElasTestTJob(Run<?, ?> run, OutputStream logger) throws JsonParseException, 
  JsonMappingException, ClientHandlerException, UniformInterfaceException, IOException{
	  
	  ExternalJob externalJob = new ExternalJob(run.getParent().getDisplayName());	
	  externalJob.settJobExecId(0L);
	  ObjectMapper objetMapper = new ObjectMapper();
	  Client client = Client.create();	  
	  	  
	  LogstashInstallation.Descriptor pluginDescriptor = LogstashInstallation.getLogstashDescriptor();
	  String elastestHostURL = pluginDescriptor.elasTestUrl + "/api/external/tjob";	  
	  WebResource webResource = client.resource(elastestHostURL);
	  ClientResponse response = webResource.type("application/json").post(ClientResponse.class, externalJob.toJSON());		
	  externalJob = objetMapper.readValue(response.getEntity(String.class), ExternalJob.class);
	  
	  if (externalJob.getExecutionUrl().indexOf("http") < 0 ){
		  externalJob.setExecutionUrl(pluginDescriptor.elasTestUrl + externalJob.getExecutionUrl());
		  externalJob.setLogAnalyzerUrl(pluginDescriptor.elasTestUrl + externalJob.getLogAnalyzerUrl());
	  }
	  
	  logger.write(("ElasTest Test Url:" + externalJob.getExecutionUrl()).getBytes());
	  logger.flush();
	  logger.write("\n".getBytes());
	  logger.flush();
	  logger.write(("ElasTest Log Analyser Url:" + externalJob.getLogAnalyzerUrl()).getBytes());	  
	  logger.flush();
	  logger.write("\n".getBytes());
	  logger.flush();
	  
	  return externalJob;
  }

  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  // Method to encapsulate calls for unit-testing
  LogstashWriter getLogStashWriter(AbstractBuild<?, ?> build, OutputStream errorStream, ExternalJob externalJob) {
    return new LogstashWriter(build, errorStream, null, externalJob);
  }

  /**
   * Registers {@link LogstashBuildWrapper} as a {@link BuildWrapper}.
   */
  @Extension
  public static class DescriptorImpl extends BuildWrapperDescriptor {

    public DescriptorImpl() {
      super(LogstashBuildWrapper.class);
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
