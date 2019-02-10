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
package jenkins.plugins.elastest.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;

public class Shell implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final Logger log = LoggerFactory.getLogger(Shell.class);

    public static Process run(final String... command) {
        return run(true, command);
    }

    public static Process run(boolean redirectOutputs,
            final String... command) {
        log.debug("[elastest-plugin]: Running command on the shell -> {}",
                Arrays.toString(command));

        try {
            ProcessBuilder p = new ProcessBuilder(command);
            p.redirectErrorStream(true);
            if (redirectOutputs) {
                p.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            }
            return p.start();
        } catch (IOException e) {
            throw new RuntimeException("Exception while executing command '"
                    + Arrays.toString(command) + "'", e);
        }
    }

    public static String runAndWaitString(final String command) {
        return runAndWait(command.split(" "));
    }

    public static String runAndWaitArray(final String[] command) {
        log.debug("[elastest-plugin]: Running command on the shell -> {}",
                Arrays.toString(command));
        String result = runAndWaitNoLog(command);
        log.info("[elastest-plugin]: Result -> " + result);
        return result;
    }

    public static String runAndWait(final String... command) {
        return runAndWaitArray(command);
    }

    public static String runAndWaitNoLog(final String... command) {
        Process p;
        try {
            p = new ProcessBuilder(command).redirectErrorStream(true).start();

            String output = CharStreams.toString(
                    new InputStreamReader(p.getInputStream(), "UTF-8"));

            p.destroy();

            return output;

        } catch (IOException e) {
            throw new RuntimeException(
                    "Exception executing command on the shell: "
                            + Arrays.toString(command),
                    e);
        }
    }

}
