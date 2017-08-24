package jenkins.plugins.elastest.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import jenkins.plugins.elastest.submiter.ElasTestIndexerDao;
import jenkins.plugins.elastest.submiter.IndexerDaoFactory;
import jenkins.plugins.elastest.submiter.ElasTestIndexerDao.IndexerType;

public class IndexerDaoFactoryTest {

  @Test
  public void getAllInstances() throws Exception {
    for (IndexerType type : IndexerType.values()) {
      String host = type == IndexerType.LOGSTASH ? "http://localhost" : "localhost";
      ElasTestIndexerDao dao = IndexerDaoFactory.getInstance(type, host, 1234, "key", "username", "password");

      assertNotNull("Result was null", dao);
      assertEquals("Result implements wrong IndexerType", type, dao.getIndexerType());
    }
  }

  @Test
  public void successNulls() throws Exception {
    for (IndexerType type : IndexerType.values()) {
      String host = type == IndexerType.LOGSTASH ? "http://localhost" : "localhost";
      ElasTestIndexerDao dao = IndexerDaoFactory.getInstance(type, host, null, "key", null, null);

      assertNotNull("Result was null", dao);
      assertEquals("Result implements wrong IndexerType", type, dao.getIndexerType());
    }
  }

  @Test(expected = InstantiationException.class)
  public void failureNullType() throws Exception {
    try {
      IndexerDaoFactory.getInstance(null, "localhost", 1234, "key", "username", "password");
    } catch (InstantiationException e) {
      String msg = "[logstash-plugin]: Unknown IndexerType 'null'. Did you forget to configure the plugin?";
      assertEquals("Wrong message", msg, e.getMessage());
      throw e;
    }
  }
}
