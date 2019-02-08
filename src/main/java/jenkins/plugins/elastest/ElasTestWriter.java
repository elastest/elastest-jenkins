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

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;

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
public class ElasTestWriter implements Serializable {
    private static final long serialVersionUID = 1L;
    transient final Logger LOG = getLogger(lookup().lookupClass());

    transient OutputStream errorStream;
    transient final Run<?, ?> build;
    final TaskListener listener;
    final String jenkinsUrl;
    transient final ElasTestSubmitter elastestSubmiter;
    private boolean connectionBroken;
    final ExternalJob externalJob;
    transient private ExecutorService executor;

    public ElasTestWriter(Run<?, ?> run, /*OutputStream error,*/
            TaskListener listener, ExternalJob externalJob) {
        LOG.info("[elastest-plugin]: Creating ElasTestWriter");
        //this.errorStream = error != null ? error : System.err;
        this.build = run;
        this.listener = listener;
        this.externalJob = externalJob;
        this.elastestSubmiter = this.getSubmitterOrNull(SubmitterType.LOGSTASH);

        if (this.elastestSubmiter == null) {
            this.jenkinsUrl = "";
        } else {
            this.jenkinsUrl = getJenkinsUrl();
        }

        executor = Executors.newSingleThreadExecutor();
    }
    
    public void setErrorStream(OutputStream error) {
        this.errorStream = error != null ? error : System.err;
    }

    /**
     * Sends a logstash payload for a single line to the indexer. Call will be
     * ignored if the line is empty or if the connection to ElasTest is broken.
     * If write fails, errors will logged to errorStream and connectionBroken
     * will be set to true.
     *
     * @param line Message, not null
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
        String host = "";
        Integer port = 0;

        if (type.compareTo(SubmitterType.LOGSTASH) == 0) {
            LOG.info("[elastest-plugin]: ElasTest services ip ->"
                    + externalJob.getServicesIp());

            if (externalJob.isFromIntegratedJenkins()) {
                if (externalJob.getServicesIp().equals("etm")) {
                    key = "api/monitoring";
                }
            } else {
                key = SubmitterType.LOGSTASH.toString();
            }

            host = externalJob.getServicesIp();
            port = Integer.valueOf(externalJob.getLogstashPort());
            LOG.debug("[elastest-plugin]: LOGSTASH KEY -> {}", key);
            LOG.debug("[elastest-plugin]: LOGSTASH HOST -> {}", host);
            LOG.debug("[elastest-plugin]: LOGSTASH PORT -> {}", port);
        }

        return SubmitterFactory.getInstance(type, host, port, key,
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
            final String payload = elastestSubmiter.buildPayload(lines,
                    externalJob);
            LOG.debug(
                    "[elastest-plugin]: Message to send " + payload.toString());
            executor.execute(() -> sendPayload(payload));
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

    public OutputStream getErrorStream() {
        return errorStream;
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public TaskListener getListener() {
        return listener;
    }

    public ExternalJob getExternalJob() {
        return externalJob;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Write error message to errorStream
     */
    private void logErrorMessage(String msg) {
        try {
            errorStream.write(msg.getBytes(StandardCharsets.UTF_8));
            errorStream.flush();
        } catch (IOException ex) {
            // This should never happen, but if it does we just have to let it
            // go.
            ex.printStackTrace();
        }
    }

    private void sendPayload(final String payload) {
        try {
            int maxAttempts = 4;
            int attempt = 0;
            boolean sended = false;
            // LOG.info("Send message in runnable: " + payload.toString());
            while (attempt < maxAttempts && !sended) {
                if (attempt > 0) {
                    try {
                        Thread.sleep(500);

                    } catch (InterruptedException ie) {
                    }
                }
                attempt++;
                LOG.debug("[elastest-plugin]: Attempt to send {}", attempt);
                sended = elastestSubmiter.push(payload.toString());
            }
            if (attempt > 4 && !sended) {
                String msg = "[elastest-plugin]: Failed to send log data to "
                        + elastestSubmiter.getSubmitterType() + ":"
                        + elastestSubmiter.getDescription() + ".\n";
                logErrorMessage(msg);
            }
        } catch (IOException e) {
            String msg = "[elastest-plugin]: Failed to send log data to "
                    + elastestSubmiter.getSubmitterType() + ":"
                    + elastestSubmiter.getDescription() + ".\n"
                    + ExceptionUtils.getStackTrace(e);
            logErrorMessage(msg);
        }
    }
}
