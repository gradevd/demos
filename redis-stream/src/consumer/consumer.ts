import { RedisClientType } from 'redis';
import { Message, ProcessedMessage, Consumer } from '../types/types';

/**
 * Represents a Redis stream consumer that processes messages from a source stream
 * within a specific consumer group and writes processed messages to a target stream.
 */
class RedisStreamConsumer implements Consumer {
  /**
   * Constructs a RedisStreamConsumer instance.
   * @param redisClient - The Redis client instance for interacting with Redis.
   * @param id - A unique identifier for this consumer.
   * @param consumerGroupName - The name of the consumer group this consumer belongs to.
   * @param sourceStreamName - The Redis stream to consume messages from.
   * @param targetStreamName - The Redis stream to push processed messages to.
   */
  constructor(
    private readonly redisClient: RedisClientType,
    private readonly id: string,
    private readonly consumerGroupName: string,
    private readonly sourceStreamName: string,
    private readonly targetStreamName: string
  ) {}

  /**
   * Consumes messages from the source Redis stream, processes each message, and
   * acknowledges it in the stream. Processed messages are written to the target stream.
   * If no messages are available, the method waits until messages arrive or the timeout expires.
   */
  public async consume(): Promise<void> {
    const result: any = await this.redisClient.xReadGroup(
      this.consumerGroupName,
      this.id,
      [{ key: this.sourceStreamName, id: '>' }],
      { COUNT: 1, BLOCK: 2000 }
    );
    if (result) {
      const { messages } = result[0];
      for (const streamEntry of messages) {
        // Map the stream data to the message type
        const message: Message = { id: streamEntry.message['message_id'] };
        console.log(
          `Consumer ${this.id} received message ${message.id} via stream ${streamEntry.id}.`
        );

        await this.processMessage(message);
        console.log(
          `Consumer ${this.id} processed stream entry ${streamEntry.id}.`
        );

        // Acknowledge the message after processing
        await this.redisClient.xAck(
          this.sourceStreamName,
          this.consumerGroupName,
          streamEntry.id
        );
        console.log(
          `Consumer ${this.id} acknowledged stream entry ${streamEntry.id}.`
        );
      }
    } else {
      console.log(`Consumer ${this.id}: No messages to process.`);
    }
  }

  /**
   * Processes an individual message by appending additional metadata and
   * writing it to the target stream.
   * @param message - The message to process.
   */
  private async processMessage(message: Message): Promise<void> {
    const processedMessage: ProcessedMessage = {
      id: message.id,
      processedAt: new Date().toISOString(),
      processedBy: this.id,
    };

    // Store the processed message in the messages:processed stream
    await this.redisClient.xAdd(this.targetStreamName, '*', {
      processed_message: JSON.stringify(processedMessage),
    });

    // Sleep for 10ms to simulate some work
    await this.sleep(10);
  }

  /**
   * Pauses execution for a specified amount of time.
   * @param timeoutMillis - The number of milliseconds to sleep.
   */
  private async sleep(timeoutMillis: number): Promise<void> {
    await new Promise((resolve) => {
      setTimeout(resolve, timeoutMillis);
    });
  }
}
export default RedisStreamConsumer;
