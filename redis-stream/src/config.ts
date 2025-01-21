import dotenv from 'dotenv';
import { Config } from './types/types';
dotenv.config();

const config: Config = {
  numberOfConsumers: Number(process.env.NUMBER_OF_CONSUMERS ?? '1'),
  consumerGroupName: process.env.CONSUMER_GROUP_NAME ?? 'consumer-group',
  consumerIdsKey: process.env.CONSUMER_IDS_KEY ?? 'consumer-ids',
  publishedMessagesStreamName:
    process.env.PUBLISHED_MESSAGES_STREAM_NAME ?? 'messages:published',
  processedMessagesStreamName:
    process.env.PROCESSED_MESSAGES_STREAM_NAME ?? 'messages:processed',
};

export default config;
