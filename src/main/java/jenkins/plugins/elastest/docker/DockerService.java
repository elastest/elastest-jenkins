package jenkins.plugins.elastest.docker;

import java.io.Serializable;
import java.util.Arrays;

import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jenkins.plugins.elastest.utils.Shell;

public class DockerService implements Serializable{
    private static final Logger LOG = LoggerFactory
            .getLogger(DockerService.class);
    private static final long serialVersionUID = 1;
    public static final String DOCKER_HOST_BY_DEFAULT = "unix:///var/run/docker.sock";
    private static DockerService dockerService;
    
    private DockerService (String dockerHost) {
        if (dockerHost != null && !dockerHost.isEmpty()) {
            
        }
    }
    
    public synchronized static DockerService getDockerService(String dockerHost) {
        if (dockerService != null) {
            return dockerService;
        } else {
            return new DockerService(dockerHost);
        }
    }
    
    public String executeDockerCommand(String... startCommand) {
        LOG.info("Docker command to execute: {}", Arrays.toString(startCommand));
        String result = Shell.runAndWait(startCommand).replaceAll("\n", "");
        return result;
    }
}
