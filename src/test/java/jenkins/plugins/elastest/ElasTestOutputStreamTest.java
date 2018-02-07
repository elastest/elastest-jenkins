package jenkins.plugins.elastest;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import jenkins.plugins.elastest.ElasTestOutputStream;
import jenkins.plugins.elastest.ElasTestWriter;

@SuppressWarnings("resource")
@RunWith(MockitoJUnitRunner.class)
public class ElasTestOutputStreamTest {
  // Extension of the unit under test that avoids making calls to getInstance() to get the DAO singleton
  static ElasTestOutputStream createLogstashOutputStream(OutputStream delegate, ElasTestWriter logstash) {
    return new ElasTestOutputStream(delegate, logstash);
  }

  ByteArrayOutputStream buffer;
  @Mock ElasTestWriter mockWriter;

  @Before
  public void before() throws Exception {
    buffer = new ByteArrayOutputStream();
    Mockito.doNothing().when(mockWriter).write(anyString());
    when(mockWriter.isConnectionBroken()).thenReturn(false);
  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockWriter);
    buffer.close();
  }

  @Test
  public void constructorSuccess() throws Exception {
    new ElasTestOutputStream(buffer, mockWriter);

    // Verify results
    assertEquals("Results don't match", "", buffer.toString());
  }

  @Test
  public void eolSuccess() throws Exception {
    ElasTestOutputStream los = new ElasTestOutputStream(buffer, mockWriter);
    String msg = "test";
    buffer.reset();

    // Unit under test
    los.eol(msg.getBytes(), msg.length());

    // Verify results
    assertEquals("Results don't match", msg, buffer.toString());
    verify(mockWriter).isConnectionBroken();
    verify(mockWriter).write(msg);
  }

  @Test
  public void eolSuccessConnectionBroken() throws Exception {
    ElasTestOutputStream los = new ElasTestOutputStream(buffer, mockWriter);

    String msg = "[logstash-plugin]: Failed to send log data to REDIS:localhost:8080.\n" +
      "[logstash-plugin]: No Further logs will be sent.\n" +
      "java.io.IOException: BOOM!";

    buffer.reset();

    // Unit under test
    los.eol(msg.getBytes(), msg.length());

    // Verify results
    assertEquals("Results don't match", msg, buffer.toString());

    // Break the dao connnection after this write
    buffer.reset();

    // Unit under test
    los.eol(msg.getBytes(), msg.length());

    // Verify results
    assertEquals("Results don't match", msg, buffer.toString());
    when(mockWriter.isConnectionBroken()).thenReturn(true);

    // Verify logs still write but on further calls are made to dao
    buffer.reset();
    // Unit under test
    los.eol(msg.getBytes(), msg.length());

    // Verify results
    for (String msgLine : msg.split("\n")) {
      assertThat("Results don't match", buffer.toString(), containsString(msgLine));
    }

    //Verify calls were made to the dao logging twice, not three times.
    verify(mockWriter, times(2)).write(msg);
    verify(mockWriter, times(3)).isConnectionBroken();
  }

  @Test
  public void eolSuccessNoDao() throws Exception {
    when(mockWriter.isConnectionBroken()).thenReturn(true);
    ElasTestOutputStream los = new ElasTestOutputStream(buffer, mockWriter);
    String msg = "test";
    buffer.reset();

    // Unit under test
    los.eol(msg.getBytes(), msg.length());

    // Verify results
    assertEquals("Results don't match", msg, buffer.toString());
    verify(mockWriter).isConnectionBroken();
  }
}
