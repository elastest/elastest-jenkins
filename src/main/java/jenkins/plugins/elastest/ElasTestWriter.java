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
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.plugins.elastest.action.ElasTestItemMenuAction;
import jenkins.plugins.elastest.json.ExternalJob;
import jenkins.plugins.elastest.submiter.BuildData;
import jenkins.plugins.elastest.submiter.ElasTestIndexerDao;
import jenkins.plugins.elastest.submiter.ElasTestIndexerDao.IndexerType;
import jenkins.plugins.elastest.submiter.IndexerDaoFactory;

/**
 * A writer that wraps all Logstash DAOs. Handles error reporting and per build
 * connection state. Each call to write (one line or multiple lines) sends a
 * Logstash payload to the DAO. If any write fails, writer will not attempt to
 * send any further messages to logstash during this build.
 *
 * @author Rusty Gerard
 * @author Liam Newman
 * @author Francisco R. Díaz
 * @since ElasTest Plugin 0.0.1
 */
public class ElasTestWriter {
	private static final Logger LOG = Logger.getLogger(ElasTestWriter.class.getName());

	final OutputStream errorStream;
	final Run<?, ?> build;
	final TaskListener listener;
	final BuildData buildData;
	final String jenkinsUrl;
	final ElasTestIndexerDao elasticSearchDao;

	private boolean connectionBroken;
	final ExternalJob externalJob;

	public ElasTestWriter(Run<?, ?> run, OutputStream error, TaskListener listener, ExternalJob externalJob) {
		this.errorStream = error != null ? error : System.err;
		this.build = run;
		this.listener = listener;
		this.externalJob = externalJob;
		this.elasticSearchDao = this.getDaoOrNull(IndexerType.LOGSTASH);

		if (this.elasticSearchDao == null) {
			this.jenkinsUrl = "";
			this.buildData = null;
		} else {
			this.jenkinsUrl = getJenkinsUrl();
			this.buildData = getBuildData();
		}
	}

	/**
	 * Sends a logstash payload for a single line to the indexer. Call will be
	 * ignored if the line is empty or if the connection to the indexer is
	 * broken. If write fails, errors will logged to errorStream and
	 * connectionBroken will be set to true.
	 *
	 * @param line
	 *            Message, not null
	 */
	public void write(String line) {
		if (!isConnectionBroken() && StringUtils.isNotEmpty(line)) {
			this.write(Arrays.asList(line));
		}
	}

	/**
	 * Sends a logstash payload containing log lines from the current build.
	 * Call will be ignored if the connection to the indexer is broken. If write
	 * fails, errors will logged to errorStream and connectionBroken will be set
	 * to true.
	 *
	 * @param maxLines
	 *            Maximum number of lines to be written. Negative numbers mean
	 *            "all lines".
	 */
	public void writeBuildLog(int maxLines/* , ExternalJob externalJob */) {
		if (!isConnectionBroken()) {
			// FIXME: build.getLog() won't have the last few lines like
			// "Finished: SUCCESS" because this hasn't returned yet...
			List<String> logLines;
			try {
				if (maxLines < 0) {
					logLines = build.getLog(Integer.MAX_VALUE);
				} else {
					logLines = build.getLog(maxLines);
				}
			} catch (IOException e) {
				String msg = "[logstash-plugin]: Unable to serialize log data.\n" + ExceptionUtils.getStackTrace(e);
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
	ElasTestIndexerDao getDao(IndexerType type) throws InstantiationException {
		ElasTestInstallation.Descriptor descriptor = ElasTestInstallation.getLogstashDescriptor();
		String key = "";

		if (type.compareTo(IndexerType.LOGSTASH) == 0) {
			key =  descriptor.logstashPath != "" ? descriptor.logstashPath : "";
		}

		return IndexerDaoFactory.getInstance(type, externalJob.getServicesIp(),
				new Integer(externalJob.getLogstashPort()), key, descriptor.username, descriptor.password);

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
	private void write(List<String> lines/* , ExternalJob externalJob */) {
		if (build.getAction(ElasTestItemMenuAction.class) != null) {
			String payload = elasticSearchDao.buildPayload(lines, externalJob);
			try {
				LOG.info("Send message: " + payload.toString());
				elasticSearchDao.push(payload.toString());
			} catch (IOException e) {
				String msg = "[logstash-plugin]: Failed to send log data to " + elasticSearchDao.getIndexerType() + ":"
						+ elasticSearchDao.getDescription() + ".\n"
						+ "[logstash-plugin]: No Further logs will be sent to " + elasticSearchDao.getDescription()
						+ ".\n" + ExceptionUtils.getStackTrace(e);
				logErrorMessage(msg);
			}
		}
	}

	/**
	 * Construct a valid indexerDao or return null. Writes errors to errorStream
	 * if dao constructor fails.
	 *
	 * @return valid {@link ElasTestIndexerDao} or return null.
	 */
	private ElasTestIndexerDao getDaoOrNull(IndexerType type) {
		try {
			return getDao(type);
		} catch (InstantiationException e) {
			String msg = ExceptionUtils.getMessage(e) + "\n"
					+ "[logstash-plugin]: Unable to instantiate LogstashIndexerDao with current configuration.\n";

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
			// This should never happen, but if it does we just have to let it
			// go.
			ex.printStackTrace();
		}
	}
}
