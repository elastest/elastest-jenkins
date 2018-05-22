package jenkins.plugins.elastest.json;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Build implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @JsonProperty("workspace")
    String workspace;
    
    public Build() {
        
    }
    
    public Build(String workspace) {
        this.workspace = workspace;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }
}
