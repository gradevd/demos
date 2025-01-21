import redis from './client/redis';
import config from './config';
import RedisStreamConsumer from './consumer/consumer';
import { Consumer } from './types/types';
import { v4 as uuidv4 } from 'uuid';

// Creates a consumer group if non-existing.
const createConsumerGroup = async () => {
  try {
    await redis.xGroupCreate(
      config.publishedMessagesStreamName,
      config.consumerGroupName,
      '$',
      { MKSTREAM: true }
    );
    console.log(
      `Consumer group ${config.consumerGroupName} created successfully.`
    );
  } catch (err: any) {
    if (err.message.includes('BUSYGROUP')) {
      console.log(
        `Consumer group ${config.consumerGroupName} already created.`
      );
    } else {
      console.error('Error creating consumer group:', err);
    }
  }
};

// Creates a pre-configured number of consumers for processing messages from a Redis stream
const initializeConsumers = async () => {
  // Cleanup prevous records
  redis.del(config.consumerIdsKey);
  // Num of consumers from CLI, or .env file
  const numConsumers: number = process.argv[2]
    ? parseInt(process.argv[2])
    : config.numberOfConsumers;
  const consumerPromises: Promise<void>[] = [];

  for (let i = 0; i < numConsumers; i++) {
    // Push the consumerId to a redis list
    const consumerId: string = uuidv4();
    redis.rPush(config.consumerIdsKey, consumerId);

    const consumer: Consumer = new RedisStreamConsumer(
      redis,
      consumerId,
      config.consumerGroupName,
      config.publishedMessagesStreamName,
      config.processedMessagesStreamName
    );
    // Run each consumer asynchronously in a loop
    const consumePromise = (async () => {
      while (true) {
        await consumer.consume();
      }
    })();
    consumerPromises.push(consumePromise);
  }
  await Promise.all(consumerPromises);
};

(async () => {
  if (!redis.isOpen) {
    await redis.connect();
  }
  try {
    await createConsumerGroup();
    await initializeConsumers();
  } finally {
    if (redis.isOpen) {
      console.log('Shutting down');
      await redis.quit();
    }
  }
})();
