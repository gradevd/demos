# Account-Mapper
## Summary
`Account-Mapper` is a WEB application that maps an external account (currently only 
GITHUB is supported) to a Freshdesk contact by creating / editing the contact
via the public Freshdesk API.
## Valuable features
`Account-Mapper` is a task-based service that executes its job on the background,
allowing the user to immediately get a response from the create request and not
be blocked on the remote API calls issues.

Once a task for an account creation is created, it is immediately returned to
the user and stored in the application's database.

A background thread, the `taskScheduler`, takes care of triggering each task
execution in a separate thread. A task is assigned to each available thread in
a synchronized way to prevent multiple threads to operate on the same task 
concurrently.

Another background thread, the `taskCleaner`, on the other hand, is responsible
to clean up the database from completed tasks and prevent memory consumption 
issues.

Additionally, `Account-Mapper` provides a fallback mechanism for tasks that
have failed in a `recoverable` way, say due to slow, or temporarily unavailable
3rd party API. A task that failed in a `recoverable` way goes in `TO_RETRY`
state, allowing the `taskScheduler` to assign the task again for execution on
another pass.

In order to prevent an endless task retries, the `Account-Mapper` also 
implements a `progressive timeout` and a `maximum number of retries` for the
tasks.

The timeout for a task to be retried is calculated via the formula:
`timeout = initial_timeout + (attempt * timeout_step)`

All parameters of the formula are configurable via environment variables.
## How to build and run
* As a docker image
  * Dependencies: Docker runtime
  * `docker-compose up --build`
  * Currently, there is a unresolved configuration issue with running the Integration test as 
    part of the Docker container. Until the issue is resolved, the maven tests
    are disabled from the docker build and must be executed via maven.
* Locally on a hosting machine
  * Dependencies: Java 17, mongo
  * Export the GitHub API key as ENV variable: `export GITHUB_API_KEY=`
  * Export the Freshdesk API key as ENV variable: `export FRESHDESK_API_KEY=`
  * Build the application: `mvnw clean install`
  * Run the app in WEB mode: `java -jar target/account-mapper-1.0.0.0.jar`
  * Run the app in CLI mode: `java -jar target/account-mapper-1.0.0.0.jar -Dspring.profiles.active=cli`

Once the Application starts, start playing with the API:
```
curl -k -X POST -H 'Content-Type: application/json' \
-d '{"account": "test", "origin": "GITHUB", "freshdeskDomain": "mytestcorp-help"}' \
https://localhost:8443/tasks
```