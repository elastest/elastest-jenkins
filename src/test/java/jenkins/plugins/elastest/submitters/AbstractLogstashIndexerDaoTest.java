package jenkins.plugins.elastest.submitters;

import static net.sf.json.test.JSONAssert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import net.sf.json.JSONObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import jenkins.plugins.elastest.submitters.AbstractElasTestSubmitter;
import jenkins.plugins.elastest.submitters.BuildData;

@RunWith(MockitoJUnitRunner.class)
public class AbstractLogstashIndexerDaoTest {
  static final String EMPTY_STRING = "{\"@buildTimestamp\":\"2000-01-01\",\"data\":{},\"message\":[],\"source\":\"jenkins\",\"source_host\":\"http://localhost:8080/jenkins\",\"@version\":1}";
  static final String ONE_LINE_STRING = "{\"@buildTimestamp\":\"2000-01-01\",\"data\":{},\"message\":[\"LINE 1\"],\"source\":\"jenkins\",\"source_host\":\"http://localhost:8080/jenkins\",\"@version\":1}";
  static final String TWO_LINE_STRING = "{\"@buildTimestamp\":\"2000-01-01\",\"data\":{},\"message\":[\"LINE 1\", \"LINE 2\"],\"source\":\"jenkins\",\"source_host\":\"http://localhost:8080/jenkins\",\"@version\":1}";

  @Mock BuildData mockBuildData;

  @Before
  public void before() throws Exception {
    when(mockBuildData.toJson()).thenReturn(JSONObject.fromObject("{}"));
    when(mockBuildData.getTimestamp()).thenReturn("2000-01-01");
  }

  @Test
  public void buildPayloadSuccessEmpty() throws Exception {
    AbstractElasTestSubmitter dao = getInstance();

    // Unit under test
    JSONObject result = dao.buildPayload(mockBuildData, "http://localhost:8080/jenkins", new ArrayList<String>(), null);
    result.remove("@timestamp");

    // Verify results
    assertEquals("Results don't match", JSONObject.fromObject(EMPTY_STRING), result);
  }

  @Test
  public void buildPayloadSuccessOneLine() throws Exception {
    AbstractElasTestSubmitter dao = getInstance();

    // Unit under test
    JSONObject result = dao.buildPayload(mockBuildData, "http://localhost:8080/jenkins", Arrays.asList("LINE 1"), null);
    result.remove("@timestamp");

    // Verify results
    assertEquals("Results don't match", JSONObject.fromObject(ONE_LINE_STRING), result);
  }

  @Test
  public void buildPayloadSuccessTwoLines() throws Exception {
    AbstractElasTestSubmitter dao = getInstance();

    // Unit under test
    JSONObject result = dao.buildPayload(mockBuildData, "http://localhost:8080/jenkins", Arrays.asList("LINE 1", "LINE 2"), null);
    result.remove("@timestamp");

    // Verify results
    assertEquals("Results don't match", JSONObject.fromObject(TWO_LINE_STRING), result);
  }

  private AbstractElasTestSubmitter getInstance() {
    return new AbstractElasTestSubmitter("localhost", -1, "", "", "") {

      public SubmitterType getSubmitterType() {
        return SubmitterType.LOGSTASH;
      }

      public void push(String data) throws IOException {}
    };
  }
}
