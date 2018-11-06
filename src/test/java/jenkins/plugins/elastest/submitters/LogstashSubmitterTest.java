package jenkins.plugins.elastest.submitters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.net.URI;

import org.apache.commons.lang.CharEncoding;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LogstashSubmitterTest {
    LogstashSubmitter submitter;
    @Mock
    HttpClientBuilder mockClientBuilder;
    @Mock
    CloseableHttpClient mockHttpClient;
    @Mock
    StatusLine mockStatusLine;
    @Mock
    CloseableHttpResponse mockResponse;
    @Mock
    HttpEntity mockEntity;

    LogstashSubmitter createSubmitter(String host, int port, String key,
            String username, String password) {
        return new LogstashSubmitter(mockClientBuilder, host, port, key,
                username, password);
    }

    @Before
    public void before() throws Exception {
        int port = (int) (Math.random() * 1000);
        submitter = createSubmitter("localhost", port, "logstash", "username",
                "password");

        when(mockClientBuilder.build()).thenReturn(mockHttpClient);
        when(mockHttpClient.execute(any(HttpPost.class)))
                .thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
    }

    @After
    public void after() throws Exception {
        // verifyNoMoreInteractions(mockClientBuilder);
        // verifyNoMoreInteractions(mockHttpClient);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorFailNullHost() throws Exception {
        try {
            createSubmitter(null, 8200, "logstash", "username", "password");
        } catch (IllegalArgumentException e) {
            assertEquals("Wrong error message was thrown",
                    "host name is required", e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorFailEmptyHost() throws Exception {
        try {
            createSubmitter("http:// ", 8200, "logstash", "username",
                    "password");
        } catch (IllegalArgumentException e) {
            assertEquals("Wrong error message was thrown",
                    "Could not create uri", e.getMessage());
            throw e;
        }
    }

    @Test
    public void constructorSuccess1() throws Exception {
        // Unit under test
        submitter = createSubmitter("localhost", 8200, "logstash", "username",
                "password");

        // Verify results
        assertEquals("Wrong host name", "localhost", submitter.host);
        assertEquals("Wrong port", 8200, submitter.port);
        assertEquals("Wrong key", "logstash", submitter.key);
        assertEquals("Wrong name", "username", submitter.username);
        assertEquals("Wrong password", "password", submitter.password);
        assertEquals("Wrong auth", "dXNlcm5hbWU6cGFzc3dvcmQ=", submitter.auth);
        assertEquals("Wrong uri", new URI("http://localhost:8200/logstash/"),
                submitter.uri);
    }

    @Test
    public void constructorSuccess2() throws Exception {
        // Unit under test
        submitter = createSubmitter("localhost", 8200, "jenkins/logstash", "",
                "password");

        // Verify results
        assertEquals("Wrong host name", "localhost", submitter.host);
        assertEquals("Wrong port", 8200, submitter.port);
        assertEquals("Wrong key", "jenkins/logstash", submitter.key);
        assertEquals("Wrong name", "", submitter.username);
        assertEquals("Wrong password", "password", submitter.password);
        assertEquals("Wrong auth", null, submitter.auth);
        assertEquals("Wrong uri",
                new URI("http://localhost:8200/jenkins/logstash/"),
                submitter.uri);
    }

    @Test
    public void getPostSuccessNoAuth() throws Exception {
        String json = "{ 'foo': 'bar' }";
        submitter = createSubmitter("localhost", 8200, "jenkins/logstash", "",
                "");

        // Unit under test
        HttpPost post = submitter.getHttpPost(json);
        HttpEntity entity = post.getEntity();

        assertEquals("Wrong uri",
                new URI("http://localhost:8200/jenkins/logstash/"),
                post.getURI());
        assertEquals("Wrong auth", 0, post.getHeaders("Authorization").length);
        assertEquals("Wrong content type", entity.getContentType().getValue(),
                ContentType.APPLICATION_JSON.toString());
        assertTrue("Wrong content class", entity instanceof StringEntity);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        entity.writeTo(stream);
        assertEquals("Wrong content", stream.toString(CharEncoding.UTF_8),
                "{ 'foo': 'bar' }");
    }

    @Test
    public void getPostSuccessAuth() throws Exception {
        String json = "{ 'foo': 'bar' }";
        submitter = createSubmitter("localhost", 8200, "logstash", "username",
                "password");

        // Unit under test
        HttpPost post = submitter.getHttpPost(json);
        HttpEntity entity = post.getEntity();

        assertEquals("Wrong uri", new URI("http://localhost:8200/logstash/"),
                post.getURI());
        assertEquals("Wrong auth", 1, post.getHeaders("Authorization").length);
        assertEquals("Wrong auth value", "Basic dXNlcm5hbWU6cGFzc3dvcmQ=",
                post.getHeaders("Authorization")[0].getValue());

        assertEquals("Wrong content type", entity.getContentType().getValue(),
                ContentType.APPLICATION_JSON.toString());
        assertTrue("Wrong content class", entity instanceof StringEntity);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        entity.writeTo(stream);
        assertEquals("Wrong content", stream.toString(CharEncoding.UTF_8),
                "{ 'foo': 'bar' }");
    }

    @Test
    public void pushSuccess() throws Exception {
        String json = "{ 'foo': 'bar' }";
        submitter = createSubmitter("http://localhost", 8200,
                "/jenkins/logstash", "", "");

        when(mockStatusLine.getStatusCode()).thenReturn(201);

        // Unit under test
        submitter.push(json);

        verify(mockClientBuilder).build();
        verify(mockHttpClient).execute(any(HttpPost.class));
        verify(mockStatusLine, atLeastOnce()).getStatusCode();
        verify(mockResponse).close();
        verify(mockHttpClient).close();
    }

    @Test
    public void pushFailStatusCode() throws Exception {
        String json = "{ 'foo': 'bar' }";
        submitter = createSubmitter("http://localhost", 8200,
                "/jenkins/logstash", "username", "password");

        when(mockStatusLine.getStatusCode()).thenReturn(500);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(
                "Something bad happened.", ContentType.TEXT_PLAIN));

        // Unit under test
        submitter.push(json);

        // Verify results
        verify(mockClientBuilder).build();
        verify(mockHttpClient).execute(any(HttpPost.class));
        verify(mockStatusLine, atLeastOnce()).getStatusCode();
        verify(mockResponse).close();
        verify(mockHttpClient).close();
    }
}
