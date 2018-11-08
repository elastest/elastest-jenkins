package jenkins.plugins.elastest.json;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import hudson.FilePath;
import jenkins.plugins.elastest.ElasTestWriter;

public class ElasTestBuild implements Serializable {
    private static final long serialVersionUID = 1L;

    private FilePath workspace;

    private ExternalJob externalJob;
    
    private List<String> containers;
    
    private transient ElasTestWriter writer;

    public ElasTestBuild() {
    }

    public ElasTestBuild(StepContext context) throws Exception {
        this(context, null);
    }

    public ElasTestBuild(StepContext context, ExternalJob externalJob)
            throws Exception {
        this(context != null ? context.get(FilePath.class) : null, externalJob);
    }

    public ElasTestBuild(FilePath workspace, ExternalJob externalJob) {
        super();
        this.workspace = workspace;
        // this.workspace.mkdirs();
        this.externalJob = externalJob;
        this.containers = new ArrayList<>();
    }

    public ExternalJob getExternalJob() {
        return externalJob;
    }

    public void setExternalJob(ExternalJob externalJob) {
        this.externalJob = externalJob;
    }

    public FilePath getWorkspace() {
        return workspace;
    }

    public void setWorkspace(FilePath workspace) {
        this.workspace = workspace;
    }

    public List<String> getContainers() {
        return containers;
    }

    public void setContainers(List<String> containers) {
        this.containers = containers;
    }

    public ElasTestWriter getWriter() {
        return writer;
    }

    public void setWriter(ElasTestWriter writer) {
        this.writer = writer;
    }


}
