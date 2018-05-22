package jenkins.plugins.elastest.json;

import java.io.Serializable;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import hudson.FilePath;

public class ElasTestBuild implements Serializable {
    private static final long serialVersionUID = 1L;
    
    FilePath workspace;    
    
    ExternalJob externalJob;
 
    public ElasTestBuild() {        
    }
        
    public ElasTestBuild(StepContext context) throws Exception {
        this(context, null);                
    }
    
    public ElasTestBuild(StepContext context, ExternalJob externalJob) throws Exception {
        this(context != null ? context.get(FilePath.class) : null, externalJob);                
    }
    
    public ElasTestBuild(FilePath workspace, ExternalJob externalJob) {
        super();
        this.workspace = workspace;
        //this.workspace.mkdirs();
        this.externalJob = externalJob;
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

}
