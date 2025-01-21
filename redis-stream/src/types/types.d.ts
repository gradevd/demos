export interface Message {
  id: string;
}

export interface ProcessedMessage extends Message {
  processedAt: string;
  processedBy: string;
}

export interface Consumer {
  consume(): Promise<void>;
}

export interface Monitor {
  monitor(): Promise<void>;
}

export interface Config {
  numberOfConsumers: number;
  consumerGroupName: string;
  consumerIdsKey: string;
  publishedMessagesStreamName: string;
  processedMessagesStreamName: string;
}
