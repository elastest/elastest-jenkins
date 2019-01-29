/*
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

import static com.google.common.collect.Ranges.closedOpen;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.common.collect.Range;

/**
 * Logstash submitter.
 *
 * @author Francisco R. Díaz
 * @since 0.0.1
 */
public class LogstashSubmitter extends AbstractElasTestSubmitter {
    private transient final Logger logger = getLogger(lookup().lookupClass());

    final HttpClientBuilder clientBuilder;
    final URI uri;
    final String auth;
    final Range<Integer> successCodes = closedOpen(200, 300);

    // primary constructor used by indexer factory
    public LogstashSubmitter(String host, int port, String key, String username,
            String password) {
        this(null, host, port, key, username, password);
    }

    LogstashSubmitter(HttpClientBuilder factory, String host, int port,
            String key, String username, String password) {
        super(host, port, key, username, password);
        logger.info("[elastest-plugin]: Creating a Logstash submitter.");

        try {
            uri = new URIBuilder("http://" + host).setPort(port)
                    .setPath("/" + key + "/").build();
            logger.info("[elastest-plugin]: Logstash URI: {}", uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Could not create uri", e);
        }

        if (StringUtils.isNotBlank(username)
                && StringUtils.isNotBlank(password)) {
            logger.info("[elastest-plugin]: Using basic authentication.");
            auth = Base64.encodeBase64String(
                    (username + ":" + StringUtils.defaultString(password))
                            .getBytes(StandardCharsets.UTF_8));
        } else {
            auth = null;
        }

        clientBuilder = factory == null ? HttpClientBuilder.create() : factory;
    }

    HttpPost getHttpPost(String data) {
        HttpPost postRequest;
        RequestConfig.Builder requestConfig = RequestConfig.custom();
        requestConfig.setConnectTimeout(3 * 1000);
        requestConfig.setConnectionRequestTimeout(3 * 1000);
        requestConfig.setSocketTimeout(3 * 1000);

        postRequest = new HttpPost(uri);
        postRequest.setConfig(requestConfig.build());
        StringEntity input = new StringEntity(data,
                ContentType.APPLICATION_JSON);
        postRequest.setEntity(input);
        if (auth != null) {
            postRequest.addHeader("Authorization", "Basic " + auth);
        }
        return postRequest;
    }

    @Override
    public boolean push(String data) throws IOException {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        HttpPost post = getHttpPost(data);
        boolean sentMessage = false;

        try {
            httpClient = clientBuilder.build();
            response = httpClient.execute(post);

            if (!successCodes
                    .contains(response.getStatusLine().getStatusCode())) {
                throw new IOException(this.getErrorMessage(response));
            }
            sentMessage = true;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            logger.error(
                    "[elastest-plugin]: Error sendind log trace message {} ",
                    data);
        } finally {
            if (response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return sentMessage;
    }

    private String getErrorMessage(CloseableHttpResponse response) {
        ByteArrayOutputStream byteStream = null;
        PrintStream stream = null;
        try {
            byteStream = new ByteArrayOutputStream();
            stream = new PrintStream(byteStream, false,
                    StandardCharsets.UTF_8.name());
            try {
                stream.print("HTTP error code: ");
                stream.println(response.getStatusLine().getStatusCode());
                stream.print("URI: ");
                stream.println(uri.toString());
                stream.println("RESPONSE: " + response.toString());
                response.getEntity().writeTo(stream);
            } catch (IOException e) {
                stream.println(ExceptionUtils.getStackTrace(e));
            }
            stream.flush();
            return byteStream.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            return "Error creating error message.";
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    @Override
    public SubmitterType getSubmitterType() {
        return SubmitterType.LOGSTASH;
    }
}
