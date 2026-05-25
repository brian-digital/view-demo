# block-and-signals-gds-store

This service is a component of **block-and-signals**

This service has 2 purposes:

- to receive and store specific events from https://github.com/hmrc/signal-processor
- to be searchable via https://github.com/hmrc/block-and-signals-frontend

## How To Run

To start the microservice with GDS API v2 enabled on the port 21102:
```shell
sbt -Dfeature.gds-api-v2.enabled=true run
```

To start the microservice with UI test configuration:
```shell
sbt -Dapplication.router=testOnlyDoNotUseInAppConf.Routes run
```


To start the microservice with local performance smoke test configuration:
```shell
sbt -Dapplication.router=testOnlyDoNotUseInAppConf.Routes -Dsearch.result.limit=9999999 run
```

### Service Manager

To start the service with its dependent services:

```shell
sm2 --start --workers 4 OBST_ALL
```

## Testing

To run the Tests:

```shell
sbt clean coverageOn test it/test coverageOff coverageAggregate
```

## Routes

| Method | Path                    | Description                                                               |
|--------|-------------------------|---------------------------------------------------------------------------|
| POST   | /:version/event         | Accepts events to be saved                                                |
| POST   | /search/count           | Returns count of records based on search criteria                         |
| POST   | /search/results         | Returns results based on search criteria                                  |
| POST   | :version/search/count   | Returns count of records based on search criteria for event type :version |
| POST   | :version/search/results | Returns results based on search criteria for event type :version          |

### POST /:version/event

Currently, the only supported version in PROD is `v1`, see work-in-progress `v2` support notes in a section below 

| Status                    | Description                                | Scenario |
|---------------------------|--------------------------------------------|----------|
| 201 CREATED               | The event has been successfully received   | Success  |
| 400 BAD_REQUEST           | Supplied body is not in the correct format | Failure  |
| 500 INTERNAL_SERVER_ERROR | The event was not successfully processed   | Failure  |

**Example Requests**

Note: the initiatingEntity is enum: admin, user, policy, analyst, system

Account Concern (including all optional fields):
```json
{
  "metadata": {
    "signalsEventType": "accountConcern",
    "originalEventType": "ORIGINAL_EVENT_TYPE",
    "jti" : "756E69717565206964656E746966696572",
    "iat" : 1730392175
  },
  "details": {
    "subjectId": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
    "credId": "3434343434343434",
    "initiatingEntity": "analyst",
    "reason": "account-takeover",
    "rationale": "RA99",
    "eventTimestampMs": 1507644997001,
    "startTimeMs": 1507644997001,
    "endTimeMs": 1507644997001
  }
}
```

Account Intervention (including all optional fields):
```json
{
  "metadata": {
    "signalsEventType": "accountIntervention",
    "originalEventType": "ORIGINAL_EVENT_TYPE",
    "jti" : "756E69717565206964656E746966696572",
    "iat" : 1730392175
  },
  "details": {
    "subjectId": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
    "credId": "3434343434343434",
    "initiatingEntity": "analyst",
    "state":  "active",
    "action" : "re-prove_identity",
    "eventTimestampMs": 1507644997001
  }
}
```

Credential Compromise (including all optional fields):
```json
{
  "metadata": {
    "signalsEventType": "credential-compromise",
    "originalEventType": "ORIGINAL_EVENT_TYPE",
    "jti" : "756E69717565206964656E746966696572",
    "iat" : 1730392175
  },
  "details": {
    "initiatingEntity": "analyst",
    "subjectId": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
    "credId": "3434343434343434",
    "credentialType": "email",
    "eventTimestampMs": 1507644997001,
    "reasonAdmin": "mfa email mismatch",
    "reasonUser": "mfa email mismatch",
    "emailAddress": "test@example.com",
    "interventionCode": "04"
  }
}
```

**Example Response**

```
201
```

### POST /v2/event

work-in-progress `v2` (GDS Inbound Signal API v2) endpoint (can be enabled by `feature.gds-api-v2.enabled` toggle)

Content-Type: application/json

**Example Response**

```
{
  "metadata": {
    "jti" : "test"
  }
}
```

| Status            | Response Body                 | Description                                                                  | Scenario |
|-------------------|:------------------------------|------------------------------------------------------------------------------|----------|
| 201 CREATED       | None                          | The request has been accepted                                                | Success  |
| 400 (Bad Request) | Only :version v1 is supported | The request was not accepted, (v2 api is disabled, or incorrect api version) | Failure  |


### POST /search/results & POST version:/search/results
For accountIntervention: only return that have the value of "analyst" in the initiatingEntity field

| Status                  | Description                                                                             | Scenario |
|-------------------------|-----------------------------------------------------------------------------------------|----------|
| 200 OK                  | 1..100,000 search results inside the response payload                                   | Success  |
| 204 NoContent           | 0 records were found for the search criteria                                            | Success  |
| 417 ExpectationFailed   | Record count exceeded 100,000, search aborted, search count inside the response payload | Failure  |
| 400 BadRequest          | Invalid payload                                                                         | Failure  |
| 500 InternalServerError | INTERNAL_SERVER_ERROR                                                                   | Failure  |

**Example Request (See SearchRequest case class for validation rules)**

```
{
  "subjectIds": [
    "urn:fdc:gov.uk:2022:Ms1hGQOww035XTEnmO6rHPxHPFDbi8o3T47zvG3O6nlsopF",
    "urn:fdc:gov.uk:2022:GNKMZfrZ4GmI1DcrxaHDfb7eENUmyNPfCPBrcHIf8bO",
    "urn:fdc:gov.uk:2022:YPdTpRgIAmPNAQWDb7B5pOhtcH1GX0Cpl6QkURK",
    "urn:fdc:gov.uk:2022:ekQHphUZOQaMI7pkmrCPvZR0iwRv4pUCqdjLzV6",
    "urn:fdc:gov.uk:2022:uFR0Jc6eboy00EHcKuJCfsBydmc9mToRyM6nvZcWjVUFcXjNWW",
    "urn:fdc:gov.uk:2022:whjGvK9NioAXNnUFjDVULRE3G9gRXeosC1HiH90tCwl8sWnuOx",
    "urn:fdc:gov.uk:2022:3uBYkK62UWlaw4k8x9AZ8OYunvCDq5P9Jkz9eaDtibq3RB",
    "urn:fdc:gov.uk:2022:7BCx0Yh5LUBpyHBXM7rYCwQxhF4g5L4ZuXIGv5WPPov",
    "urn:fdc:gov.uk:2022:jwYk0aCcgzchGh0ge01yvXeluGtymHxNN920c2gRW",
    "urn:fdc:gov.uk:2022:usLSsESeMnBeRgR3ygj4ZIoSLRyOfnsuURdQtskgkffvgildjw"
  ],
  "credIds": [
    "credId-5f7d5d8f-08af-43e7-96d3-df027fcca680",
    "credId-352da9f3-1fc6-4071-bf7a-c5a5cfc9ff8b",
    "credId-c2b62bdd-239b-455d-b5eb-f30d75937332",
    "credId-32d213d4-7d81-49c2-adf1-16006baa219a",
    "credId-c4f2a2ad-3881-48bd-a22f-1c3b2511c2ab",
    "credId-5b445314-8f7e-4217-b37a-d1eee7b193f5",
    "credId-e4b1b75d-eaef-4981-9de1-6a825a4e056e",
    "credId-41acabcf-d3c5-474c-b42a-8d0a3c781c00",
    "credId-f8b47470-fab4-4508-a9fc-b69feae8db23",
    "credId-4266b421-c8b5-4aa5-9c38-aafde16c1d8f"
  ],
  "eventType": "account-concern",
  "dateFrom": "2025-01-25",
  "dateTo": "2025-01-25"
}
```

**Example Response (Chunked/Streamed)**

JSON lines

```
{"credId":"1234567891012345","eventType":"account-concern","reason":"coercion","subjectId":"urn:123...","rationale":"RA01","timeOfInterest":"2009-02-01T02:53:09Z","action":"suspended"}
{"credId":"1234567891012345","eventType":"account-intervention","reason":"account-takeover","subjectId":"urn:123...","rationale":"RA04","timeOfInterest":"2009-02-01T02:53:09Z","action":"permanently_suspended"}
{"credId":"1234567891012345","eventType":"account-intervention","reason":"User aborted face to face session","subjectId":"urn:123...","rationale":"RA02","timeOfInterest":"2009-02-01T02:53:09Z","action":"re-prove_identity"}
```

### POST /search/count & POST version:/search/count

For accountIntervention: only count that have the value of "analyst" in the initiatingEntity field


| Status                  | Description                                                                                               | Scenario |
|-------------------------|-----------------------------------------------------------------------------------------------------------|----------|
| 200 OK                  | 200 OK / Returns a count of the number of records that would be returned if the query was run as a search | Success  |
| 400 BadRequest          | Invalid payload                                                                                           | Failure  |
| 500 InternalServerError | INTERNAL_SERVER_ERROR                                                                                     | Failure  |

**Example Request (See SearchRequest case class for validation rules)**

```
{
  "subjectIds": [
    "urn:fdc:gov.uk:2022:Ms1hGQOww035XTEnmO6rHPxHPFDbi8o3T47zvG3O6nlsopF",
    "urn:fdc:gov.uk:2022:GNKMZfrZ4GmI1DcrxaHDfb7eENUmyNPfCPBrcHIf8bO",
    "urn:fdc:gov.uk:2022:YPdTpRgIAmPNAQWDb7B5pOhtcH1GX0Cpl6QkURK",
    "urn:fdc:gov.uk:2022:ekQHphUZOQaMI7pkmrCPvZR0iwRv4pUCqdjLzV6",
    "urn:fdc:gov.uk:2022:uFR0Jc6eboy00EHcKuJCfsBydmc9mToRyM6nvZcWjVUFcXjNWW",
    "urn:fdc:gov.uk:2022:whjGvK9NioAXNnUFjDVULRE3G9gRXeosC1HiH90tCwl8sWnuOx",
    "urn:fdc:gov.uk:2022:3uBYkK62UWlaw4k8x9AZ8OYunvCDq5P9Jkz9eaDtibq3RB",
    "urn:fdc:gov.uk:2022:7BCx0Yh5LUBpyHBXM7rYCwQxhF4g5L4ZuXIGv5WPPov",
    "urn:fdc:gov.uk:2022:jwYk0aCcgzchGh0ge01yvXeluGtymHxNN920c2gRW",
    "urn:fdc:gov.uk:2022:usLSsESeMnBeRgR3ygj4ZIoSLRyOfnsuURdQtskgkffvgildjw"
  ],
  "credIds": [
    "credId-5f7d5d8f-08af-43e7-96d3-df027fcca680",
    "credId-352da9f3-1fc6-4071-bf7a-c5a5cfc9ff8b",
    "credId-c2b62bdd-239b-455d-b5eb-f30d75937332",
    "credId-32d213d4-7d81-49c2-adf1-16006baa219a",
    "credId-c4f2a2ad-3881-48bd-a22f-1c3b2511c2ab",
    "credId-5b445314-8f7e-4217-b37a-d1eee7b193f5",
    "credId-e4b1b75d-eaef-4981-9de1-6a825a4e056e",
    "credId-41acabcf-d3c5-474c-b42a-8d0a3c781c00",
    "credId-f8b47470-fab4-4508-a9fc-b69feae8db23",
    "credId-4266b421-c8b5-4aa5-9c38-aafde16c1d8f"
  ],
  "eventType": "account-concern",
  "dateFrom": "2025-01-25",
  "dateTo": "2025-01-25"
}
```

**Example Response**

```
{
  "count":3
}
```


## Test Only Routes (Inc routes for v2 events)

| Method | Path                | Description                      |
|--------|---------------------|----------------------------------|
| POST   | /test-only/event    | Insert randomly generated events |
| DELETE | /test-only/event    | Delete inserted events           |
| POST   | /test-only/v2/event | Insert randomly generated events |
| DELETE | /test-only/v2/event | Delete inserted events           |

### POST /test-only/event & POST /test-only/v2/event

This endpoint adds randomly generated events via a bulk insert operation.

When events are created, each event `eventId` field is prefixed with `test-` to allow targeted deletion.

The `eventCount` field in the request body determines how many events are inserted.

| Status                    | Response Body                   | Description                                                                                                           | Scenario |
|---------------------------|---------------------------------|-----------------------------------------------------------------------------------------------------------------------|----------|
| 201 Created               | None                            | The request has been successfully processed                                                                           | Success  |
| 204 No Content            | None                            | The request was not successfully processed due to the `test-only.event-generator.enabled` feature flag being disabled | Failure  |
| 400 Bad Request           | Play exception message          | Supplied body is not in the correct format                                                                            | Failure  |
| 500 Internal Server Error | Play or mongo exception message | The request was not successfully processed                                                                            | Failure  |

**Example Request one**

The following request inserts 100k randomly generated events.

```
{
    "eventCount": 100000
}
```

**Example Request two**

The following request inserts 100k randomly generated events but credIds is in order.
When randomlyGenerate is false - if to create 10,000 records, 10 records will have credId 0000000000000001, 10 records will have credId 0000000000000002 etc.
When updateView is true, the gdsSearchView will be updated for each event inserted.
When v2EventsAvailable is true, deviceConcern and credentialConcern signals are available to be chosen by the generator.

```
{
    "eventCount": 100000,
    "dateFrom": "2025-06-25T15:35:31.621337Z", //default value (now - 18 months)
    "dateTo": "2025-06-25T15:35:31.621341Z", //default value now
    "randomlyGenerate": true, //default value true
    "updateView": true, // Optional, defaults to true
    "v2EventsAvailable": false // Optional, defaults to false
}
```

### DELETE /test-only/event & DELETE /test-only/v2/event

This endpoint deletes **all** events, to be used on small dataset otherwise queries naturally batch up, bottle neck and cause other services to suffer on the platform.

| Status                    | Response Body                   | Description                                 | Scenario |
|---------------------------|:--------------------------------|---------------------------------------------|----------|
| 204 No Content            | None                            | The request has been successfully processed | Success  |
| 500 Internal Server Error | Play or mongo exception message | The request was not successfully processed  | Failure  |

### DELETE /test-only/clearDB & DELETE /test-only/v2/clearDB

Matching the api on `block-and-signals`. Sets TTL of all records in DB to x minutes, utilised mongo's daemon to delete on our behalf.
Submits with unacknowledged write concern across the replicasets as instant feedback is not required.

| Status                    | Response Body                   | Description                                 | Scenario |
|---------------------------|:--------------------------------|---------------------------------------------|----------|
| 204 No Content            | None                            | The request has been successfully processed | Success  |
| 500 Internal Server Error | Play or mongo exception message | The request was not successfully processed  | Failure  |

### Config keys and there meaning
| Key                                          | Description                                                                                                                            |
|----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| feature.gds-api-v2.enabled                   | GDS Inbound Signal API v2 is enabled                                                                                                   |
| search.result.limit                          | The number of results that we allow before throwing a 417                                                                              |
| search.result.mongo-batch-size               | The number of documents to be returned per batch during a search.                                                                      |
| search.result.chunk-size                     | Used to chunk up the result stream. chunk size represents the limit of how big each chunk of elements can be                           |
| search.result.chunk-window                   | Used to chunk up the result stream. chunk window is used to represent the amount of time that can pass before a chunk must be created. |
| search.request.max-allowed-ids               | The maximum number of total Ids that can be used for a search.                                                                         |
| search.request.max-allowed-cred-ids          | The maximum number of Cred Ids that can be used for a search.                                                                          |
| search.request.max-allowed-subject-ids       | The maximum number of Subject Ids can be used for a search.                                                                            |
| test-only.event-generator.insert-parallelism | The number of futures to be run in parallel when inserting data for the testing.                                                       |
| materialized-view.gds-search.create          | should the GDS Search View be created / dropped and recreated when the service starts                                                  |
