import redis from './client/redis';
import config from './config';
import RedisStreamMonitor from './monitor/monitor';
import { Monitor } from './types/types';

// Main function to initialize and run the monitor.
(() => {
  const monitor: Monitor = new RedisStreamMonitor(
    redis,
    config.processedMessagesStreamName,
    3000
  );
  monitor.monitor();
})();
