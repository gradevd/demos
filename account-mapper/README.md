# Account-Mapper

## Summary
`Account-Mapper` is a WEB application that maps an external account (currently only 
GITHUB is supported) to a Freshdesk contact by creating / editing the contact
via the public Freshdesk API.

## How to build and run
* As a docker image
  * Dependencies: Docker runtime
  * `docker-compose up --build`
  * Currently, there is an unresolved configuration issue with running the Integration test as 
    part of the Docker container. Until the issue is resolved, the maven tests
    are disabled from the docker build and must be executed via maven.
* Locally on a hosting machine
  * Dependencies: Java 17, mongo
  * Export the GitHub API key as ENV variable: `export GITHUB_API_KEY=`
  * Export the Freshdesk API key as ENV variable: `export FRESHDESK_API_KEY=`
  * Build the application: `mvnw clean install`
  * Run the app in WEB mode: `java -jar target/account-mapper-1.0.0.0.jar`
  * Run the app in CLI mode: `java -jar target/account-mapper-1.0.0.0.jar --spring.profiles.active=cli --command.line.interface.enabled=true`

Once the Application starts, start experimenting with the API.
## Create a task
```
curl -k -X POST -H 'Content-Type: application/json' \
-d '{"account": "test", "origin": "GITHUB", "freshdeskDomain": "mytestcorp-help"}' \
https://localhost:8443/tasks
```
## List all available tasks
```
curl -k -X GET -H 'Content-Type: application/json' https://localhost:8443/tasks
```

## Account Mapper Architectural Overview
![AccountMapper](diagram.png)

## How it works
`Account-Mapper` is a task-based service that executes its job on the background,
allowing the user to immediately get a response from the create request and not
be blocked on the remote API calls.

Once a task for an account creation is submitted, it is immediately returned to
the user and stored in the application's database.

A background thread, the `taskScheduler`, takes care of triggering each task
execution in a separate thread. A task is assigned to an available thread in
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

## TODOs:
### A simple UI against the existing REST API:
* Dialog for creating a task to map an account
  * Origin account (only GitHub)
  * Account name
  * Freshdesk sub-domain
  * Submit / Cancel buttons
* Task Console (e.g. in the form of a footer)
  * List all task, ordered by `updated` data and `status`
### Future ideas:
* Login form
  * Create a user
  * Login with an existing user
* Fields mapping
  * Parse the source and target API schemas
  * Allow the user to map each source API field to a specific target API field
  * Add validation for incompatible field mappings
* Scale up the app by introducing other forms of integrations between target and source APIs and allow the user to build a logical sequence out of these integrations
* Rename the app to match the end version of the product