package jenkins.plugins.elastest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import hudson.model.AbstractBuild;
import hudson.model.Job;
import hudson.model.Run;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import jenkins.plugins.elastest.ElasTestBuildWrapper;
import jenkins.plugins.elastest.ElasTestOutputStream;
import jenkins.plugins.elastest.ElasTestWriter;
import jenkins.plugins.elastest.persistence.BuildData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ElasTestBuildWrapperTest {
  // Extension of the unit under test that avoids making calls to statics or constructors
  static class MockLogstashBuildWrapper extends ElasTestBuildWrapper {
    ElasTestWriter writer;

    MockLogstashBuildWrapper(ElasTestWriter writer) {
      super();
      this.writer = writer;
    }

    @Override
    ElasTestWriter getLogStashWriter(AbstractBuild<?, ?> build, OutputStream errorStream, ExternalJob externalJob) {
      // Simulate bad Writer
      if(writer.isConnectionBroken()) {
        try {
          errorStream.write("Mocked Constructor failure".getBytes());
        } catch (IOException e) {
        }
      }
      return writer;
    }
  }

  ByteArrayOutputStream buffer;

  @Mock AbstractBuild<?, ?> mockBuild;  
  @Mock BuildData mockBuildData;
  @Mock ElasTestWriter mockWriter;

  @Before
  public void before() throws Exception {
    when(mockWriter.isConnectionBroken()).thenReturn(false);    
    when(mockBuild.getParent().getDisplayName()).thenReturn("JobTest");

    buffer = new ByteArrayOutputStream();
  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockWriter);
    verifyNoMoreInteractions(mockBuildData);
    buffer.close();
  }

  @Test
  public void decorateLoggerSuccess() throws Exception {
    MockLogstashBuildWrapper buildWrapper = new MockLogstashBuildWrapper(mockWriter);

    // Unit under test
    OutputStream result = buildWrapper.decorateLogger(mockBuild, buffer);

    // Verify results
    assertNotNull("Result was null", result);
    assertTrue("Result is not the right type", result instanceof ElasTestOutputStream);
    assertSame("Result has wrong writer", mockWriter, ((ElasTestOutputStream) result).logstash);
    assertEquals("Results don't match", "", buffer.toString());
    verify(mockWriter).isConnectionBroken();
  }

  @Test
  public void decorateLoggerSuccessBadWriter() throws Exception {
    when(mockWriter.isConnectionBroken()).thenReturn(true);

    MockLogstashBuildWrapper buildWrapper = new MockLogstashBuildWrapper(mockWriter);

    // Unit under test
    OutputStream result = buildWrapper.decorateLogger(mockBuild, buffer);

    // Verify results
    assertNotNull("Result was null", result);
    assertTrue("Result is not the right type", result instanceof ElasTestOutputStream);
    assertSame("Result has wrong writer", mockWriter, ((ElasTestOutputStream) result).logstash);
    assertEquals("Error was not written", "Mocked Constructor failure", buffer.toString());
    verify(mockWriter).isConnectionBroken();
  }
}
