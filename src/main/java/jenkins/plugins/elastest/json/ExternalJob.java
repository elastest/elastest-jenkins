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
package jenkins.plugins.elastest.json;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author Francisco R Díaz
 * @since 0.0.1
 */
public class ExternalJob implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("jobName")
    private String jobName;

    @JsonProperty("executionUrl")
    private String executionUrl;

    @JsonProperty("analyzerUrl")
    private String logAnalyzerUrl;

    @JsonProperty("tJobExecId")
    private Long tJobExecId;

    @JsonProperty("logstashPort")
    private String logstashPort;

    @JsonProperty("servicesIp")
    private String servicesIp;

    @JsonProperty("tSServices")
    private List<TestSupportServices> tSServices;

    @JsonProperty("envVars")
    private Map<String, String> envVars;

    @JsonProperty("result")
    private int result;

    @JsonProperty("isReady")
    private boolean isReady;

    @JsonProperty("status")
    private ExternalJobStatusEnum status;

    @JsonProperty("error")
    private String error;

    @JsonProperty("testResultFilePattern")
    private String testResultFilePattern;

    @JsonProperty("testResults")
    private List<String> testResults;

    @JsonProperty("sut")
    private Sut sut;

    @JsonProperty("fromIntegratedJenkins")
    private boolean fromIntegratedJenkins;

    @JsonProperty("buildUrl")
    private String buildUrl;

    @JsonProperty("jobUrl")
    private String jobUrl;

    public ExternalJob() {
    }

    public ExternalJob(String jobName) {
        this.jobName = jobName;
    }

    public ExternalJob(String jobName, String executionUrl,
            String logAnalyzerUrl, Long tJobExecId, String logstashPort,
            String servicesIp, List<TestSupportServices> tSServices,
            Map<String, String> envVars, int result, boolean isReady,
            ExternalJobStatusEnum status, String error,
            String testResultFilePattern, List<String> testResults, Sut sut,
            boolean fromIntegratedJenkins, String buildUrl, String jobUrl) {
        super();
        this.jobName = jobName;
        this.executionUrl = executionUrl;
        this.logAnalyzerUrl = logAnalyzerUrl;
        this.tJobExecId = tJobExecId;
        this.logstashPort = logstashPort;
        this.servicesIp = servicesIp;
        this.tSServices = tSServices;
        this.envVars = envVars;
        this.result = result;
        this.isReady = isReady;
        this.status = status;
        this.error = error;
        this.testResultFilePattern = testResultFilePattern;
        this.testResults = testResults;
        this.sut = sut;
        this.fromIntegratedJenkins = fromIntegratedJenkins;
        this.buildUrl = buildUrl;
        this.jobUrl = jobUrl;
    }

    public enum ExternalJobStatusEnum {
        STARTING("Starting"), READY("Ready"), ERROR("Error");

        private String value;

        ExternalJobStatusEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static ExternalJobStatusEnum fromValue(String text) {
            for (ExternalJobStatusEnum b : ExternalJobStatusEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }

    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getExecutionUrl() {
        return executionUrl;
    }

    public void setExecutionUrl(String executionUrl) {
        this.executionUrl = executionUrl;
    }

    public String getLogAnalyzerUrl() {
        return logAnalyzerUrl;
    }

    public void setLogAnalyzerUrl(String logAnalyzerUrl) {
        this.logAnalyzerUrl = logAnalyzerUrl;
    }

    public Long gettJobExecId() {
        return tJobExecId;
    }

    public void settJobExecId(Long tJobExecId) {
        this.tJobExecId = tJobExecId;
    }

    public String getLogstashPort() {
        return logstashPort;
    }

    public void setLogstashPort(String logstashPort) {
        this.logstashPort = logstashPort;
    }

    public String getServicesIp() {
        return servicesIp;
    }

    public void setServicesIp(String servicesIp) {
        this.servicesIp = servicesIp;
    }

    public List<TestSupportServices> getTSServices() {
        return tSServices;
    }

    public void setTSServices(List<TestSupportServices> tSServices) {
        this.tSServices = tSServices;
    }

    public Map<String, String> getEnvVars() {
        return envVars;
    }

    public void setEnvVars(Map<String, String> envVars) {
        this.envVars = envVars;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public ExternalJobStatusEnum getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public void setStatus(ExternalJobStatusEnum status) {
        this.status = status;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getTestResultFilePattern() {
        return testResultFilePattern;
    }

    public void setTestResultFilePattern(String testResultFilePattern) {
        this.testResultFilePattern = testResultFilePattern;
    }

    public List<String> getTestResults() {
        return testResults;
    }

    public void setTestResults(List<String> testResults) {
        this.testResults = testResults;
    }

    public Sut getSut() {
        return sut;
    }

    public void setSut(Sut sut) {
        this.sut = sut;
    }

    public boolean isFromIntegratedJenkins() {
        return fromIntegratedJenkins;
    }

    public String getBuildUrl() {
        return buildUrl;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setFromIntegratedJenkins(boolean fromIntegratedJenkins) {
        this.fromIntegratedJenkins = fromIntegratedJenkins;
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExternalJob externalJob = (ExternalJob) o;
        return Objects.equals(this.jobName, externalJob.jobName)
                && Objects.equals(this.executionUrl, externalJob.executionUrl)
                && Objects.equals(this.logAnalyzerUrl,
                        externalJob.logAnalyzerUrl)
                && Objects.equals(this.tJobExecId, externalJob.tJobExecId)
                && Objects.equals(this.logstashPort, externalJob.logstashPort)
                && Objects.equals(this.servicesIp, externalJob.servicesIp)
                && Objects.equals(this.tSServices, externalJob.tSServices)
                && this.result == externalJob.result
                && Objects.equals(this.envVars, externalJob.envVars)
                && this.isReady == externalJob.isReady
                && Objects.equals(this.testResultFilePattern,
                        externalJob.testResultFilePattern)
                && Objects.equals(this.testResults, externalJob.testResults)
                && Objects.equals(this.sut, externalJob.sut)
                && Objects.equals(this.status, externalJob.status)
                && Objects.equals(this.error, externalJob.error)
                && this.isFromIntegratedJenkins() == externalJob
                        .isFromIntegratedJenkins()
                && Objects.equals(this.buildUrl, externalJob.buildUrl)
                && Objects.equals(this.jobUrl, externalJob.jobUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobName, executionUrl, logAnalyzerUrl, tJobExecId,
                logstashPort, servicesIp, tSServices, envVars, result, isReady,
                testResultFilePattern, testResults, sut, status, error,
                fromIntegratedJenkins, buildUrl, jobUrl);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DeployConfig {\n");
        sb.append("    jobName: ").append(toIndentedString(jobName))
                .append("\n");
        sb.append("    executionUrl: ").append(toIndentedString(executionUrl))
                .append("\n");
        sb.append("    logAnalyzerUrl: ")
                .append(toIndentedString(logAnalyzerUrl)).append("\n");
        sb.append("    tJobExecId: ").append(toIndentedString(tJobExecId))
                .append("\n");
        sb.append("    logstashPort: ").append(toIndentedString(logstashPort))
                .append("\n");
        sb.append("    servicesIp: ").append(toIndentedString(servicesIp))
                .append("\n");
        sb.append("    tSServices: ").append(toIndentedString(tSServices))
                .append("\n");
        sb.append("    envVars: ").append(toIndentedString(envVars))
                .append("\n");
        sb.append("    result: ").append(toIndentedString(result)).append("\n");
        sb.append("    isReady: ").append(toIndentedString(isReady))
                .append("\n");
        sb.append("    testResultFilePattern: ")
                .append(toIndentedString(testResultFilePattern)).append("\n");
        sb.append("    testResults: ").append(toIndentedString(testResults))
                .append("\n");
        sb.append("    sut: ").append(toIndentedString(sut)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    error: ").append(toIndentedString(error)).append("\n");
        sb.append("    fromIntegratedJenkins: ")
                .append(toIndentedString(fromIntegratedJenkins)).append("\n");
        sb.append("    buildUrl: ").append(toIndentedString(buildUrl))
                .append("\n");
        sb.append("    jobBuild: ").append(toIndentedString(jobUrl))
                .append("\n");
        sb.append("}");

        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

    public String toJSON() {
        ObjectMapper mapper = new ObjectMapper();

        // Object to JSON in String
        String jsonInString;
        try {
            jsonInString = mapper.writeValueAsString(this);
            return jsonInString;

        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "";
    }

}
