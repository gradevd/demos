import redis from '../src/client/redis';
import RedisStreamConsumer from '../src/consumer/consumer'; // Adjust import according to your project structure
import { Message } from '../src/types/types'; // Adjust import according to your project structure

describe('RedisStreamConsumer', () => {
  const consumerGroupName = 'test-consumer-group';
  const sourceStreamName = 'source-stream';
  const targetStreamName = 'target-stream';

  const consumerId1 = 'consumer-1';
  const consumerId2 = 'consumer-2';

  // Connect the redis client before all tests
  beforeAll(async () => {
    await redis.connect();
  });

  // Disconnect the redis client after all tests
  afterAll(async () => {
    await redis.quit();
  });

  // Create the consumer group before each test
  beforeEach(async () => {
    try {
      await redis.xGroupCreate(sourceStreamName, consumerGroupName, '$', {
        MKSTREAM: true,
      });
    } catch (error: any) {
      // Ignore error if the consumer group already exists
      if (error.message.includes('BUSYGROUP')) {
        console.log('Consumer group already exists, continuing...');
      } else {
        throw error;
      }
    }
  });

  // Cleanup the streams after each test
  afterEach(async () => {
    await redis.del(sourceStreamName);
    await redis.del(targetStreamName);
  });

  it('should process messages correctly with a single consumer', async () => {
    // Add a message to the source stream
    const message: Message = { id: 'message-1' };
    await redis.xAdd(sourceStreamName, '*', { message_id: message.id });
    // Set up the consumer
    const consumer = new RedisStreamConsumer(
      redis,
      consumerId1,
      consumerGroupName,
      sourceStreamName,
      targetStreamName
    );

    // Let the consumer complete their job
    await consumer.consume();

    // Verify the message was acknowledged and no further pending messages are left in the stream
    const newMessages: any = await redis.xReadGroup(
      consumerGroupName,
      consumerId1,
      [{ key: sourceStreamName, id: '>' }],
      { COUNT: 1, BLOCK: 100 }
    );
    expect(newMessages).toBeNull();

    // Verify that the message was processed and added to the target stream
    const targetStreamMessages: any = await redis.xRange(
      targetStreamName,
      '-',
      '+'
    );
    expect(targetStreamMessages.length).toEqual(1);

    // Verify the processed message content
    const processedMessage = JSON.parse(
      targetStreamMessages[0].message.processed_message
    );
    expect(processedMessage.id).toBe(message.id);
    expect(processedMessage.processedBy).toBe(consumerId1);
  });

  it('should process messages correctly with multiple consumers in parallel', async () => {
    // Add two messages to the source stream
    const message1: Message = { id: 'message-1' };
    const message2: Message = { id: 'message-2' };
    await redis.xAdd(sourceStreamName, '*', { message_id: message1.id });
    await redis.xAdd(sourceStreamName, '*', { message_id: message2.id });

    // Create two consumers
    const consumer1 = new RedisStreamConsumer(
      redis,
      consumerId1,
      consumerGroupName,
      sourceStreamName,
      targetStreamName
    );
    const consumer2 = new RedisStreamConsumer(
      redis,
      consumerId2,
      consumerGroupName,
      sourceStreamName,
      targetStreamName
    );

    // Run both consumers in parallel
    const consumePromise1 = consumer1.consume();
    const consumePromise2 = consumer2.consume();
    await Promise.all([consumePromise1, consumePromise2]);

    // Check that both messages were processed and added to the target stream
    const targetStreamMessages = await redis.xRange(targetStreamName, '-', '+');
    expect(targetStreamMessages.length).toEqual(2); // Ensure both messages are processed
  });

  it('should handle an empty stream and not process any messages', async () => {
    // Set up a consumer
    const consumer = new RedisStreamConsumer(
      redis,
      consumerId1,
      consumerGroupName,
      sourceStreamName,
      targetStreamName
    );

    // Let the consumer finish their job
    await consumer.consume();

    // Verify that the consumer has not processed any messages (stream is empty)
    const targetStreamMessages = await redis.xRange(targetStreamName, '-', '+');
    expect(targetStreamMessages.length).toBe(0); // No messages should be processed
  });
});
