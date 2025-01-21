import { createClient, RedisClientType } from 'redis';

const redis: RedisClientType = createClient({
  url: 'redis://localhost:6379',
});

export default redis;
