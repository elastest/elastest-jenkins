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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jenkins.security.MasterToSlaveCallable;

/**
 * Executes a docker command on a distributed agent if necessary
 * 
 * @author Francisco R. Díaz
 * @since 0.0.1
 */
public class DockerCommandExecutor
        extends MasterToSlaveCallable<String, RuntimeException> {
    private static final Logger LOG = LoggerFactory
            .getLogger(DockerCommandExecutor.class);
    private static final long serialVersionUID = 1L;
    private String[] command;
    private DockerService dockerService;

    public DockerCommandExecutor(String[] command,
            DockerService dockerService) {
        super();
        this.command = command != null ? command.clone() : null;
        this.dockerService = dockerService;
    }

    @Override
    public String call() throws RuntimeException {
        LOG.debug(
                "[elastest-plugin]: Executing docker command \" {} \" on a distributed node if necessary",
                Arrays.toString(command));
        String result = dockerService.executeDockerCommand(command);
        LOG.debug("Docker command output: {}", result);
        return result;
    }

    public String[] getCommand() {
        return command.clone();
    }

    public void setCommand(String... command) {
        this.command = command;
    }
}
