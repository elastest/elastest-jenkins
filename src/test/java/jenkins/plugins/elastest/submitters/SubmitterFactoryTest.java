package jenkins.plugins.elastest.submitters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import jenkins.plugins.elastest.submitters.ElasTestSubmitter;
import jenkins.plugins.elastest.submitters.SubmitterFactory;
import jenkins.plugins.elastest.submitters.ElasTestSubmitter.SubmitterType;

public class SubmitterFactoryTest {

  @Test
  public void getAllInstances() throws Exception {
    for (SubmitterType type : SubmitterType.values()) {
      String host = type == SubmitterType.LOGSTASH ? "http://localhost" : "localhost";
      ElasTestSubmitter dao = SubmitterFactory.getInstance(type, host, 1234, "key", "username", "password");

      assertNotNull("Result was null", dao);
      assertEquals("Result implements wrong IndexerType", type, dao.getSubmitterType());
    }
  }

  @Test
  public void successNulls() throws Exception {
    for (SubmitterType type : SubmitterType.values()) {
      String host = type == SubmitterType.LOGSTASH ? "http://localhost" : "localhost";
      ElasTestSubmitter dao = SubmitterFactory.getInstance(type, host, null, "key", null, null);

      assertNotNull("Result was null", dao);
      assertEquals("Result implements wrong IndexerType", type, dao.getSubmitterType());
    }
  }

  @Test(expected = InstantiationException.class)
  public void failureNullType() throws Exception {
    try {
      SubmitterFactory.getInstance(null, "localhost", 1234, "key", "username", "password");
    } catch (InstantiationException e) {
      String msg = "[elastest-plugin]: Unknown IndexerType 'null'. Did you forget to configure the plugin?";
      assertEquals("Wrong message", msg, e.getMessage());
      throw e;
    }
  }
}
