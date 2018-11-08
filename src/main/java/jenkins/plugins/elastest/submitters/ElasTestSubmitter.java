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
package jenkins.plugins.elastest.submitters;

import java.io.IOException;
import java.util.List;

import jenkins.plugins.elastest.json.ExternalJob;

/**
 * 
 * @author Francisco R. Díaz
 * @since 0.0.1
 */
public interface ElasTestSubmitter {
    static enum SubmitterType {
        LOGSTASH("logstash");

        private final String name;

        private SubmitterType(String s) {
            name = s;
        }

        public boolean equalsName(String otherName) {
            return name.equals(otherName);
        }

        public String toString() {
            return this.name;
        }
    }

    String getDescription();

    SubmitterType getSubmitterType();

    /**
     * Sends the log data to ElasTest.
     *
     * @param data
     *            The serialized data, not null
     * @throws java.io.IOException
     *             The data is not written to the server
     */
    boolean push(String data) throws IOException;

    /**
     * Bulds a String playload compatible with the Logstash input.
     * 
     * @param logLines
     * @param externalJob
     * @return
     */
    String buildPayload(List<String> logLines, ExternalJob externalJob);
    
    /**
     * Bulds a String playload compatible with the Logstash input.
     * 
     * @param message
     * @param externalJob
     * @return
     */
    String buildPayload(String message, ExternalJob externalJob);
}
