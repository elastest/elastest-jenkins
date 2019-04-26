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
package jenkins.plugins.elastest.docker;

import java.io.Serializable;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jenkins.plugins.elastest.utils.Shell;

/**
 * Docker service to execute docker commands from the shell
 * @author Francisco R. Díaz
 *
 */
public class DockerService implements Serializable {
    private static final Logger LOG = LoggerFactory
            .getLogger(DockerService.class);
    private static final long serialVersionUID = 1;
    public static final String DOCKER_HOST_BY_DEFAULT = "unix:///var/run/docker.sock";
    private static DockerService dockerService;

    public synchronized static DockerService getDockerService(
            String dockerHost) {
        if (dockerService != null) {
            return dockerService;
        } else {
            return new DockerService();
        }
    }

    public String executeDockerCommand(String... startCommand) {
        LOG.info("[elastest-plugin]: Docker command to execute: {}",
                Arrays.toString(startCommand));
        String result = Shell.runAndWait(startCommand).replaceAll("\n", "");
        return result;
    }

    public String getGatewayFromContainer(String containerName) {
        String gateway = null;
        gateway = executeDockerCommand("docker", "inspect",
                "--format=\\\"{{.NetworkSettings.Networks.elastest_elastest.Gateway}}\\\"",
                containerName);
        LOG.info("[elastest-plugin]: Docker network gateway: {}", gateway);
        return gateway.replaceAll("\\\\\"", "");
    }
}
