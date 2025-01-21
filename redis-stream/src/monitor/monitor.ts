import { RedisClientType } from 'redis';
import { Monitor, ProcessedMessage } from '../types/types';

class RedisStreamMonitor implements Monitor {
  constructor(
    private readonly redis: RedisClientType,
    private readonly streamKey: string,
    private readonly intervalMillis: number
  ) {}

  public async monitor(): Promise<void> {
    if (!this.redis.isOpen) {
      await this.redis.connect();
    }
    setInterval(async () => {
      const messages: any = await this.redis.xRange(this.streamKey, '-', '+');
      if (messages.length === 0) {
        console.log('There are no processed messages.');
        return;
      }
      const messagesPerSecond: Set<number> = new Set();
      // <consumerId, <second, numOfMessages>>
      const perConsumerPerSecond: Map<string, Map<number, number>> = new Map();
      for (const { id, message } of messages) {
        // Get the timestamp
        const entryTimestamp = id.split('-')[0];
        const second = Math.floor(entryTimestamp / 1000);
        messagesPerSecond.add(second);
        // Get the processedMessage
        const processedMessage: ProcessedMessage = JSON.parse(
          message['processed_message']
        );
        const consumerMap =
          perConsumerPerSecond.get(processedMessage.processedBy) ||
          new Map<number, number>();
        consumerMap.set(second, (consumerMap.get(second) || 0) + 1);
        perConsumerPerSecond.set(processedMessage.processedBy, consumerMap);
      }
      console.log(
        `Message processing ratio [overall]: ${
          messages.length / messagesPerSecond.size
        } messages/second.\n`
      );
      perConsumerPerSecond.forEach(
        (msgPerSec: Map<number, number>, consumer: string) => {
          const seconds = msgPerSec.size;
          const messages = Array.from(msgPerSec.values()).reduce(
            (acc, current) => acc + current,
            0
          );
          console.log(
            `Message processing ratio [Consumer ${consumer}]: ${
              messages / seconds
            } messages/second.`
          );
        }
      );
      console.log('\n');
    }, this.intervalMillis);
  }
}
export default RedisStreamMonitor;
