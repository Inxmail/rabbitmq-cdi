package net.reini.rabbitmq.cdi;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

/**
 * Tests the {@link EventPublisher} implementation.
 *
 * @author Patrick Reinhart
 */
@RunWith(MockitoJUnitRunner.class)
public class EventPublisherTest {
  @Mock
  private ConnectionProducer connectionProducer;
  @Mock
  private ConnectionConfig config;
  @Mock
  private Connection connection;
  @Mock
  private Channel channel;
  @Mock
  private BiConsumer<?, PublishException> errorHandler;

  private EventPublisher publisher;
  private Builder basicProperties;
  private JsonEncoder<Object> encoder;

  @Before
  public void setUp() throws Exception {
    publisher = new EventPublisher(connectionProducer);
    basicProperties = new BasicProperties.Builder();
    encoder = new JsonEncoder<>();
  }

  /**
   * Test method for {@link EventPublisher#publishEvent(Object)}.
   */
  @Test
  public void testPublishEvent_no_configuration() {
    publisher.publishEvent(new TestEvent());
  }

  /**
   * Test method for {@link EventPublisher#addEvent(Class, PublisherConfiguration)},
   * {@link EventPublisher#publishEvent(Object)} and {@link EventPublisher#cleanUp()}.
   * 
   * @throws TimeoutException
   * @throws IOException
   */
  @SuppressWarnings("boxing")
  @Test
  public void testPublishEvent() throws IOException, TimeoutException {
    when(connectionProducer.getConnection(config)).thenReturn(connection);
    when(connection.createChannel()).thenReturn(channel);
    when(channel.isOpen()).thenReturn(true);
    
    publisher.addEvent(TestEvent.class, new PublisherConfiguration(config, "exchange", "routingKey",
        basicProperties, encoder, errorHandler));
    publisher.publishEvent(new TestEvent());
    publisher.cleanUp();

    verify(channel).basicPublish(eq("exchange"), eq("routingKey"), any(), any());
    verify(channel).close();
  }

  /**
   * Test method for {@link EventPublisher#addEvent(Class, PublisherConfiguration)},
   * {@link EventPublisher#publishEvent(Object)} and {@link EventPublisher#cleanUp()}.
   * 
   * @throws TimeoutException
   * @throws IOException
   */
  @SuppressWarnings("boxing")
  @Test
  public void testPublishEvent_failing() throws IOException, TimeoutException {
    when(connectionProducer.getConnection(config)).thenReturn(connection);
    when(connection.createChannel()).thenReturn(channel);
    when(channel.isOpen()).thenReturn(true);
    doThrow(IOException.class).when(channel).basicPublish(eq("exchange"), eq("routingKey"), any(),
        any());

    publisher.addEvent(TestEvent.class, new PublisherConfiguration(config, "exchange", "routingKey",
        basicProperties, encoder, errorHandler));
    publisher.publishEvent(new TestEvent());
    publisher.cleanUp();

    verify(channel, times(4)).close();
  }
}