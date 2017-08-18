/*
 * The MIT License
 *
 * Copyright 2014 Rusty Gerard and Liam Newman
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.plugins.elastest.persistence.BuildData;
import jenkins.plugins.elastest.persistence.IndexerDaoFactory;
import jenkins.plugins.elastest.persistence.LogstashIndexerDao;
import jenkins.plugins.elastest.persistence.LogstashIndexerDao.IndexerType;

/**
 * A writer that wraps all Logstash DAOs.  Handles error reporting and per build connection state.
 * Each call to write (one line or multiple lines) sends a Logstash payload to the DAO.
 * If any write fails, writer will not attempt to send any further messages to logstash during this build.
 *
 * @author Rusty Gerard
 * @author Liam Newman 
 * @since 1.0.5
 */
public class ElasTestWriter {

  final OutputStream errorStream;
  final Run<?, ?> build;
  final TaskListener listener;
  final BuildData buildData;
  final String jenkinsUrl;
  final LogstashIndexerDao elasticSearchDao;

  
  private boolean connectionBroken;
  final ExternalJob externalJob;

  public ElasTestWriter(Run<?, ?> run, OutputStream error, TaskListener listener, ExternalJob externalJob) {
    this.errorStream = error != null ? error : System.err;
    this.build = run;
    this.listener = listener;
    this.externalJob = externalJob;
    this.elasticSearchDao = this.getDaoOrNull(IndexerType.ELASTICSEARCH);
    
    if (this.elasticSearchDao == null){
      this.jenkinsUrl = "";
      this.buildData = null;
    } else {
      this.jenkinsUrl = getJenkinsUrl();
      this.buildData = getBuildData();
    }
  }

  /**
   * Sends a logstash payload for a single line to the indexer.
   * Call will be ignored if the line is empty or if the connection to the indexer is broken.
   * If write fails, errors will logged to errorStream and connectionBroken will be set to true.
   *
   * @param line
   *          Message, not null
   */
  public void write(String line) {
    if (!isConnectionBroken() && StringUtils.isNotEmpty(line)) {
      this.write(Arrays.asList(line));
    }
  }

  /**
   * Sends a logstash payload containing log lines from the current build.
   * Call will be ignored if the connection to the indexer is broken.
   * If write fails, errors will logged to errorStream and connectionBroken will be set to true.
   *
   * @param maxLines
   *          Maximum number of lines to be written.  Negative numbers mean "all lines".
   */
  public void writeBuildLog(int maxLines/*, ExternalJob externalJob*/) {
    if (!isConnectionBroken()) {
      // FIXME: build.getLog() won't have the last few lines like "Finished: SUCCESS" because this hasn't returned yet...
      List<String> logLines;
      try {
        if (maxLines < 0) {
          logLines = build.getLog(Integer.MAX_VALUE);
        } else {
          logLines = build.getLog(maxLines);
        }
      } catch (IOException e) {
        String msg = "[logstash-plugin]: Unable to serialize log data.\n" +
          ExceptionUtils.getStackTrace(e);
        logErrorMessage(msg);

        // Continue with error info as logstash payload
        logLines = Arrays.asList(msg.split("\n"));
      }

      write(logLines);
    }
  }

  /**
   * @return True if errors have occurred during initialization or write.
   */
  public boolean isConnectionBroken() {
    return connectionBroken || build == null || elasticSearchDao == null || buildData == null;
  }

  // Method to encapsulate calls for unit-testing
  LogstashIndexerDao getDao(IndexerType type) throws InstantiationException {
    ElasTestInstallation.Descriptor descriptor = ElasTestInstallation.getLogstashDescriptor();
    String key = "";
    
    if (type.compareTo( IndexerType.ELASTICSEARCH) == 0){
    	key = String.valueOf(externalJob.gettJobExecId()) + "/" + "testlogs";    	    	
    }
    
    return IndexerDaoFactory.getInstance(type, externalJob.getServicesIp(), new Integer(externalJob.getLogstashPort()), key , null, null);
    
  }

  BuildData getBuildData() {
    if (build instanceof AbstractBuild) {
      return new BuildData((AbstractBuild) build, new Date());
    } else {
      return new BuildData(build, new Date(), listener);
    }
  }

  String getJenkinsUrl() {
    return Jenkins.getInstance().getRootUrl();
  }

  /**
   * Write a list of lines to the indexer as one Logstash payload.
   */
  private void write(List<String> lines/*, ExternalJob externalJob*/) {   
    String payload = elasticSearchDao.buildPayload(lines, externalJob);
    try {
    	System.out.println("Send message: " + payload.toString());
    	elasticSearchDao.push(payload.toString());
    	
    	if (lines.get(0).contains("Finished:")){
    		sendEndJobMessageToElasTest();
    	}
    } catch (IOException e) {
      String msg = "[logstash-plugin]: Failed to send log data to " + elasticSearchDao.getIndexerType() + ":" + elasticSearchDao.getDescription() + ".\n" +
        "[logstash-plugin]: No Further logs will be sent to " + elasticSearchDao.getDescription() + ".\n" +
        ExceptionUtils.getStackTrace(e);
      logErrorMessage(msg);
    }
  }
  
  private void sendEndJobMessageToElasTest(){	  
	  Client client = Client.create();	  	  
	  ElasTestInstallation.Descriptor pluginDescriptor = ElasTestInstallation.getLogstashDescriptor();
	  String elasTestApiURL = pluginDescriptor.elasTestUrl + "/api/external/tjob";
	  
	  WebResource webResource = client.resource(elasTestApiURL);
	  ClientResponse response = webResource.type("application/json").put(ClientResponse.class, externalJob.toJSON());	  
  }

  /**
   * Construct a valid indexerDao or return null.
   * Writes errors to errorStream if dao constructor fails.
   *
   * @return valid {@link LogstashIndexerDao} or return null.
   */
  private LogstashIndexerDao getDaoOrNull(IndexerType type) {
    try {
      return getDao(type);
    } catch (InstantiationException e) {
      String msg =  ExceptionUtils.getMessage(e) + "\n" +
        "[logstash-plugin]: Unable to instantiate LogstashIndexerDao with current configuration.\n";

      logErrorMessage(msg);
    }
    return null;
  }

  /**
   * Write error message to errorStream and set connectionBroken to true.
   */
  private void logErrorMessage(String msg) {
    try {
      connectionBroken = true;
      errorStream.write(msg.getBytes());
      errorStream.flush();
    } catch (IOException ex) {
      // This should never happen, but if it does we just have to let it go.
      ex.printStackTrace();
    }
  }
}
