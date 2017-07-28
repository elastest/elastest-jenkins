/*
 * The MIT License
 *
 * Copyright 2014 Rusty Gerard
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
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.plugins.elastest.ExternalJob;
import jenkins.plugins.elastest.TJob;
import jenkins.tasks.SimpleBuildStep;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.kohsuke.stapler.DataBoundConstructor;
import org.jenkinsci.Symbol;

/**
 * Post-build action to push build log to Logstash.
 *
 * @author Rusty Gerard
 * @since 1.0.0
 */
public class LogstashNotifier extends Notifier implements SimpleBuildStep {

  public int maxLines;
  public boolean failBuild;

  @DataBoundConstructor
  public LogstashNotifier(int maxLines, boolean failBuild) {
    this.maxLines = maxLines;
    this.failBuild = failBuild;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
	  System.out.println("Execution order: 2");
    return perform(build, listener);
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher,
    TaskListener listener) throws InterruptedException, IOException {
    if (!perform(run, listener)) {
      run.setResult(Result.FAILURE);
    }
  }

  private boolean perform(Run<?, ?> run, TaskListener listener) {
	  System.out.println("Execution order: 3");
    PrintStream errorPrintStream = listener.getLogger();
    try{    	
	    ExternalJob externalJob = asociateToElasTestTJob(run, listener);
	    LogstashWriter logstash = getLogStashWriter(run, errorPrintStream, listener, externalJob);
	    logstash.writeBuildLog(maxLines);
	    return !(failBuild && logstash.isConnectionBroken());
    }catch (Exception e){
    	e.printStackTrace();
		listener.getLogger().println("Error:"+e.getMessage());
		return false;
    }    
  }

  // Method to encapsulate calls for unit-testing
  LogstashWriter getLogStashWriter(Run<?, ?> run, OutputStream errorStream, TaskListener listener, ExternalJob externalJob) {
    return new LogstashWriter(run, errorStream, listener, externalJob);
  }
  
  private ExternalJob asociateToElasTestTJob(Run<?, ?> run, TaskListener listener) throws JsonParseException, JsonMappingException, 
  ClientHandlerException, UniformInterfaceException, IOException{
	  ExternalJob externalJob = new ExternalJob(run.getParent().getDisplayName());	
	  externalJob.settJobExecId(0L);
	  ObjectMapper objetMapper = new ObjectMapper();
	  Client client = Client.create();	  
	  	  
	  LogstashInstallation.Descriptor pluginDescriptor = LogstashInstallation.getLogstashDescriptor();
	  String elastestHostURL = "http://" + pluginDescriptor.elasTestUrl +"/api/external/tjob";
	  System.out.println("ElasTest URL: " + elastestHostURL);
	  listener.getLogger().println("ElasTest URL: " + elastestHostURL);
	  WebResource webResource = client.resource(elastestHostURL);
	  ClientResponse response = webResource.type("application/json").post(ClientResponse.class, externalJob.toJSON());		
	  externalJob = objetMapper.readValue(response.getEntity(String.class), ExternalJob.class);
	  listener.getLogger().println("ElasTest Test Url:" + externalJob.getExecutionUrl());	
	  
	  return externalJob;
  }

  public BuildStepMonitor getRequiredMonitorService() {
    // We don't call Run#getPreviousBuild() so no external synchronization between builds is required
    return BuildStepMonitor.NONE;
  }

  @Override
  public Descriptor getDescriptor() {
    return (Descriptor) super.getDescriptor();
  }

  @Extension @Symbol("logstashSend")
  public static class Descriptor extends BuildStepDescriptor<Publisher> {

    @Override
    public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
      return true;
    }

    public String getDisplayName() {
      return Messages.DisplayName();
    }
  }
}
