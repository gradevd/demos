# Redist-Stream

## Summary
`redis-stream` is a Node.js application that runs a configurable number of consumers in parallel. The consumers read messages from a redis stream with key `messages:published` via a predefined consumer group ensuring the messages are distributed to all consumers running in parallel with no duplications.
As part of the message processing, the consumers append additional information to the message received, such as `id`, `processedBy` and `processedAt`, and push the processed messages to another stream with key `messages:processed`.
Additionally, there is also a monitoring logic implemented, that periodically pulls the messages from the `messages:processed` stream and generates statistics of the messages processing ratio - overall and per consumer.

A Python script acts as the publisher.

## Prerequisites
* Node
* Python 3

## How to build and run
* `clone` the repo
* Run `npm install`
* Run `npm run consumers-dev -- ${n}` to run `n` consumers in parallel
* Run `npm run monitor-dev` to start the monitor
* Install redis dependency for python: `pip3 install redis`
* Run the publisher: `python 3 publisher.py`
* Run the test suites: `npm test`
