package jenkins.plugins.elastest;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import hudson.model.AbstractBuild;
import hudson.model.Project;
import hudson.model.Result;
import hudson.tasks.test.AbstractTestResultAction;
import jenkins.plugins.elastest.submiter.BuildData;
import jenkins.plugins.elastest.submiter.ElasTestIndexerDao;
import jenkins.plugins.elastest.submiter.ElasTestIndexerDao.IndexerType;
import net.java.sezpoz.impl.Indexer6;
import net.sf.json.JSONObject;

@RunWith(MockitoJUnitRunner.class)
public class LogstashWriterTest {
  // Extension of the unit under test that avoids making calls to getInstance() to get the DAO singleton
  static ElasTestWriter createElasTestWriter(final AbstractBuild<?, ?> testBuild,
                                             OutputStream error,
                                             final String url,
                                             final ElasTestIndexerDao indexer,
                                             final BuildData data) {
    return new ElasTestWriter(testBuild, error, null, null) {
      @Override
      ElasTestIndexerDao getDao(IndexerType type) throws InstantiationException {
        if (indexer == null) {
          throw new InstantiationException("DoaTestInstantiationException");
        }

        return indexer;
      }

      @Override
      BuildData getBuildData() {        

        // For testing, providing null data means use the actual method
        if (data == null) {
          return super.getBuildData();
        } else {
          return data;
        }
      }

      @Override
      String getJenkinsUrl() {
        return url;
      }
    };
  }

  ByteArrayOutputStream errorBuffer;

  @Mock ElasTestIndexerDao mockDao;
  @Mock AbstractBuild mockBuild;
  @Mock AbstractTestResultAction mockTestResultAction;
  @Mock Project mockProject;

  @Mock BuildData mockBuildData;

  @Captor ArgumentCaptor<List<String>> logLinesCaptor;

  @Before
  public void before() throws Exception {

    when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
    when(mockBuild.getDisplayName()).thenReturn("LogstashNotifierTest");
    when(mockBuild.getProject()).thenReturn(mockProject);
    when(mockBuild.getBuiltOn()).thenReturn(null);
    when(mockBuild.getNumber()).thenReturn(123456);
    when(mockBuild.getDuration()).thenReturn(0L);
    when(mockBuild.getTimestamp()).thenReturn(new GregorianCalendar());
    when(mockBuild.getRootBuild()).thenReturn(mockBuild);
    when(mockBuild.getBuildVariables()).thenReturn(Collections.emptyMap());
    when(mockBuild.getSensitiveBuildVariables()).thenReturn(Collections.emptySet());
    when(mockBuild.getEnvironments()).thenReturn(null);
    when(mockBuild.getAction(AbstractTestResultAction.class)).thenReturn(mockTestResultAction);
    when(mockBuild.getLog(0)).thenReturn(Arrays.asList());
    when(mockBuild.getLog(3)).thenReturn(Arrays.asList("line 1", "line 2", "line 3", "Log truncated..."));
    when(mockBuild.getLog(Integer.MAX_VALUE)).thenReturn(Arrays.asList("line 1", "line 2", "line 3", "line 4"));

    when(mockTestResultAction.getTotalCount()).thenReturn(0);
    when(mockTestResultAction.getSkipCount()).thenReturn(0);
    when(mockTestResultAction.getFailCount()).thenReturn(0);
    when(mockTestResultAction.getFailedTests()).thenReturn(Collections.emptyList());

    when(mockProject.getName()).thenReturn("LogstashWriterTest");

    when(mockDao.buildPayload(Matchers.anyListOf(String.class), Matchers.any(ExternalJob.class)))
      .thenReturn("");

    Mockito.doNothing().when(mockDao).push(Matchers.anyString());
    when(mockDao.getIndexerType()).thenReturn(IndexerType.LOGSTASH);
    when(mockDao.getDescription()).thenReturn("localhost:8080");

    errorBuffer = new ByteArrayOutputStream();
  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockDao);
    verifyNoMoreInteractions(mockBuild);
    verifyNoMoreInteractions(mockBuildData);
    verifyNoMoreInteractions(mockTestResultAction);
    verifyNoMoreInteractions(mockProject);
    errorBuffer.close();
  }

  @Test
  public void constructorSuccess() throws Exception {
    createElasTestWriter(mockBuild, errorBuffer, "http://my-jenkins-url", mockDao, null);

    // Verify that the BuildData constructor is what is being called here.
    // This also lets us verify that in the instantiation failure cases we do not construct BuildData.
    verify(mockBuild).getId();
    verify(mockBuild, times(2)).getResult();
    verify(mockBuild, times(2)).getParent();
    verify(mockBuild, times(2)).getDisplayName();
    verify(mockBuild).getFullDisplayName();
    verify(mockBuild).getDescription();
    verify(mockBuild).getUrl();
    verify(mockBuild).getAction(AbstractTestResultAction.class);
    verify(mockBuild).getBuiltOn();
    verify(mockBuild, times(2)).getNumber();
    verify(mockBuild).getTimestamp();
    verify(mockBuild, times(3)).getRootBuild();
    verify(mockBuild).getBuildVariables();
    verify(mockBuild).getSensitiveBuildVariables();
    verify(mockBuild).getEnvironments();

    verify(mockTestResultAction).getTotalCount();
    verify(mockTestResultAction).getSkipCount();
    verify(mockTestResultAction).getFailCount();
    verify(mockTestResultAction, times(2)).getFailedTests();

    verify(mockProject, times(2)).getName();

    // Verify results
    assertEquals("Results don't match", "", errorBuffer.toString());
  }

  @Test
  public void constructorSuccessNoDao() throws Exception {
    String exMessage = "InstantiationException: DoaTestInstantiationException\n" +
      "[logstash-plugin]: Unable to instantiate LogstashIndexerDao with current configuration.\n";

    // Unit under test
    ElasTestWriter writer = createElasTestWriter(mockBuild, errorBuffer, "http://my-jenkins-url", null, null);

    // Verify results
    assertEquals("Results don't match", exMessage, errorBuffer.toString());
    assertTrue("Connection not broken", writer.isConnectionBroken());
  }

  @Test
  public void writeSuccessNoDao() throws Exception {
    ElasTestWriter writer = createElasTestWriter(mockBuild, errorBuffer, "http://my-jenkins-url", null, null);
    assertTrue("Connection not broken", writer.isConnectionBroken());

    String msg = "test";
    errorBuffer.reset();

    // Unit under test
    writer.write(msg);

    // Verify results
    assertEquals("Results don't match", "", errorBuffer.toString());
    assertTrue("Connection not broken", writer.isConnectionBroken());
  }

  @Test
  public void writeBuildLogSuccessNoDao() throws Exception {
    ElasTestWriter writer = createElasTestWriter(mockBuild, errorBuffer, "http://my-jenkins-url", null, null);
    assertTrue("Connection not broken", writer.isConnectionBroken());

    errorBuffer.reset();

    // Unit under test
    writer.writeBuildLog(3);

    // Verify results
    assertEquals("Results don't match", "", errorBuffer.toString());
    assertTrue("Connection not broken", writer.isConnectionBroken());
  }

  @Test
  public void writeSuccess() throws Exception {
    ElasTestWriter writer = createElasTestWriter(mockBuild, errorBuffer, "http://my-jenkins-url", mockDao, mockBuildData);
    String msg = "test";
    errorBuffer.reset();

    // Unit under test
    writer.write(msg);

    // Verify results
    // No error output
    assertEquals("Results don't match", "", errorBuffer.toString());

    verify(mockDao).buildPayload(Matchers.anyListOf(String.class), Matchers.any(ExternalJob.class));
    verify(mockDao).push("");
  }

  @Test
  public void writeBuildLogSuccess() throws Exception {
    ElasTestWriter writer = createElasTestWriter(mockBuild, errorBuffer, "http://my-jenkins-url", mockDao, mockBuildData);
    errorBuffer.reset();

    // Unit under test
    writer.writeBuildLog(3);

    // Verify results
    // No error output
    assertEquals("Results don't match", "", errorBuffer.toString());
    verify(mockBuild).getLog(3);

    verify(mockDao).buildPayload(Matchers.anyListOf(String.class), Matchers.any(ExternalJob.class));
    verify(mockDao).push("");
  }

  @Test
  public void writeSuccessConnectionBroken() throws Exception {
    Mockito.doNothing().doThrow(new IOException("BOOM!")).doNothing().when(mockDao).push(anyString());
    ElasTestWriter los = createElasTestWriter(mockBuild, errorBuffer, "http://my-jenkins-url", mockDao, mockBuildData);


    String msg = "test";
    String exMessage = "[logstash-plugin]: Failed to send log data to REDIS:localhost:8080.\n" +
      "[logstash-plugin]: No Further logs will be sent to localhost:8080.\n" +
      "java.io.IOException: BOOM!";

    errorBuffer.reset();

    // Unit under test
    los.write(msg);

    // Verify results
    assertEquals("Results don't match", "", errorBuffer.toString());

    // Break the dao connnection
    errorBuffer.reset();

    // Unit under test
    los.write(msg);

    // Verify results
    assertTrue("Results don't match", errorBuffer.toString().startsWith(exMessage));
    assertTrue("Connection not broken", los.isConnectionBroken());

    // Verify logs still write but on further calls are made to dao
    errorBuffer.reset();
    // Unit under test
    los.write(msg);

    // Verify results
    assertEquals("Results don't match", "", errorBuffer.toString());

    //Verify calls were made to the dao logging twice, not three times.
    verify(mockDao, times(2)).buildPayload(Matchers.anyListOf(String.class), Matchers.any(ExternalJob.class));
    verify(mockDao, times(2)).push("");
    verify(mockDao).getIndexerType();
    verify(mockDao, times(2)).getDescription();
  }

  @Test
  public void writeBuildLogGetLogError() throws Exception {
    // Initialize mocks
    when(mockBuild.getLog(3)).thenThrow(new IOException("Unable to read log file"));

    ElasTestWriter writer = createElasTestWriter(mockBuild, errorBuffer, "http://my-jenkins-url", mockDao, mockBuildData);
    assertEquals("Errors were written", "", errorBuffer.toString());

    // Unit under test
    writer.writeBuildLog(3);

    // Verify results
    verify(mockBuild).getLog(3);

    List<String> expectedErrorLines =  Arrays.asList(
      "[logstash-plugin]: Unable to serialize log data.",
      "java.io.IOException: Unable to read log file");
    verify(mockDao).push("");
    verify(mockDao).buildPayload(Matchers.anyListOf(String.class), Matchers.any(ExternalJob.class));
    List<String> actualLogLines = logLinesCaptor.getValue();

    assertThat("The exception was not sent to Logstash", actualLogLines.get(0), containsString(expectedErrorLines.get(0)));
    assertThat("The exception was not sent to Logstash", actualLogLines.get(1), containsString(expectedErrorLines.get(1)));
  }
}
