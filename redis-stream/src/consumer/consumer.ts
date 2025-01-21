import { RedisClientType } from 'redis';
import { Message, ProcessedMessage, Consumer } from '../types/types';

class RedisStreamConsumer implements Consumer {
  constructor(
    private readonly redisClient: RedisClientType,
    private readonly id: string,
    private readonly consumerGroupName: string,
    private readonly sourceStreamName: string,
    private readonly targetStreamName: string
  ) {}

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

  private async sleep(timeoutMillis: number): Promise<void> {
    await new Promise((resolve) => {
      setTimeout(resolve, timeoutMillis);
    });
  }
}
export default RedisStreamConsumer;
