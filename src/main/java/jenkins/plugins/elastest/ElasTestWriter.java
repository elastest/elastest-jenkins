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

package jenkins.plugins.elastest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.plugins.elastest.action.ElasTestItemMenuAction;
import jenkins.plugins.elastest.json.ExternalJob;
import jenkins.plugins.elastest.submitters.ElasTestSubmitter;
import jenkins.plugins.elastest.submitters.ElasTestSubmitter.SubmitterType;
import jenkins.plugins.elastest.submitters.SubmitterFactory;

/**
 * A writer that wraps all submitters.
 *
 * @author Francisco R. Díaz
 * @since 0.0.1
 */
public class ElasTestWriter {
    private static final Logger LOG = Logger
            .getLogger(ElasTestWriter.class.getName());

    final OutputStream errorStream;
    final Run<?, ?> build;
    final TaskListener listener;
    final String jenkinsUrl;
    final ElasTestSubmitter elastestSubmiter;

    private boolean connectionBroken;
    final ExternalJob externalJob;

    public ElasTestWriter(Run<?, ?> run, OutputStream error,
            TaskListener listener, ExternalJob externalJob) {
        this.errorStream = error != null ? error : System.err;
        this.build = run;
        this.listener = listener;
        this.externalJob = externalJob;
        this.elastestSubmiter = this.getSubmitterOrNull(SubmitterType.LOGSTASH);

        if (this.elastestSubmiter == null) {
            this.jenkinsUrl = "";
        } else {
            this.jenkinsUrl = getJenkinsUrl();
        }
    }

    /**
     * Sends a logstash payload for a single line to the indexer. Call will be
     * ignored if the line is empty or if the connection to ElasTest is broken.
     * If write fails, errors will logged to errorStream and connectionBroken
     * will be set to true.
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
     * @return True if errors have occurred during initialization or write.
     */
    public boolean isConnectionBroken() {
        return connectionBroken || build == null || elastestSubmiter == null;
    }

    // Method to encapsulate calls for unit-testing
    ElasTestSubmitter getSubmitter(SubmitterType type)
            throws InstantiationException {
        ElasTestInstallation.Descriptor descriptor = ElasTestInstallation
                .getLogstashDescriptor();
        String key = "";

        if (type.compareTo(SubmitterType.LOGSTASH) == 0) {
            key = SubmitterType.LOGSTASH.toString();
        }

        return SubmitterFactory.getInstance(type, externalJob.getServicesIp(),
                Integer.valueOf(externalJob.getLogstashPort()), key,
                descriptor.username, descriptor.password);

    }

    String getJenkinsUrl() {
        return Jenkins.getInstance().getRootUrl();
    }

    /**
     * Write a list of lines to the indexer as one Logstash payload.
     */
    private void write(List<String> lines) {
        if (build.getAction(ElasTestItemMenuAction.class) != null) {
            String payload = elastestSubmiter.buildPayload(lines, externalJob);
            try {
                LOG.info("Send message: " + payload.toString());
                elastestSubmiter.push(payload.toString());
            } catch (IOException e) {
                String msg = "[logstash-plugin]: Failed to send log data to "
                        + elastestSubmiter.getSubmitterType() + ":"
                        + elastestSubmiter.getDescription() + ".\n"
                        + "[logstash-plugin]: No Further logs will be sent to "
                        + elastestSubmiter.getDescription() + ".\n"
                        + ExceptionUtils.getStackTrace(e);
                logErrorMessage(msg);
            }
        }
    }

    /**
     * Construct a valid indexerDao or return null. Writes errors to errorStream
     * if dao constructor fails.
     *
     * @return valid {@link ElasTestSubmitter} or return null.
     */
    private ElasTestSubmitter getSubmitterOrNull(SubmitterType type) {
        try {
            return getSubmitter(type);
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
