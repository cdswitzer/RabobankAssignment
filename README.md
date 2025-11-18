# Implementation
This implementation of the Rabobank Assignment for Authorizations Area is made by Cor Switzer.
The original assignment can be found at the bottom of this document.

Before running the application the following is required:
* Idea - Clone and load project
* Java 21
* Maven 3.9 (or later)
* Docker
* Postman

Optional:
* MongoDB Compass (if you want to take a look in de database yourself)

## Build
Because the project uses spotless to force a codestyle, spotless needs to be run as well.
```bash
mvn spotless:apply clean install
```

## Running locally
To run the application on your local machine you'll need a MongoDB database to store data.
From the root of the project run:
```bash
docker compose up
```

When the database is started you can start the application:
```bash
mvn -pl api spring-boot:run 
```

> NOTE: If you made changes after building and before running, it is possible that maven starts complaning about spotless checks.
> In that case you need to add `spotless:apply` to you mvn command. 

> NOTE: if you made any changes to the docker-compose.yaml, like username, password, database etc., 
> make sure you also apply these changes to the application.
> The `application.yaml` is found under `/data/src/main/resources/`.

## Running request
When everything is running, you should be able to execute request to the application.
If no changes are made, the API will be accessible on http://localhost:8080

### Implemented endpoints
Account:
* POST - `/api/v1/accounts`
  * Accepts a AccountRequest:
    * ```json
        {
            "accountNumber": "NL100000001",
            "accountHolderName": "John Doe",
            "accountType": "PAYMENT",
            "initialBalance": 1500.00
        }
        ```
    * Returns 201 with an account
    * Returns 409 if account already exist
* GET - `/api/v1/accounts`
  * Returns 200
    * Returns a list of all accounts if the exists, otherwise an empty list will be returned.
* GET - `/api/v1/accounts/{accountNumber}`
  * Accepts a accountNumber(String)
  * Returns 200 with an account if the account is found
  * Returns 404 if the account does not exist

PowerOfAttorney:
* POST - `/api/v1/power-of-attorney`
  * Accepts a PowerOfAttorneyRequest:
    * ```json
      {
        "grantorName": "John Doe",
        "granteeName": "Alice Cooper",
        "authorization": "READ",
        "accountNumber": "NL100000001",
        "accountType": "PAYMENT"
       }
        ```
  * Returns 201 with a power of attorney
  * Returns 404 if the account does not exist
  * Returns 403 if the grantor is not the account holder
* GET - `/api/v1/power-of-attorney`
  * Accepts an optional parameter `granteeName`
    * If provided
      * Returns 200 with all power of attorney for given grantee.
    * If not provided
      * Returns 200 with a list of all power of attorney if they exist, otherwise an empty list will be returned.

To make you life a bit easier, I provided a collection of request for the endpoints. 
The collection can be found at `src/main/resources/postman_collection/collection.json`
You can import this collection in an application like [Postman](https://www.postman.com/) and run the collections.

# Original assignment

## Rabobank Assignment for Authorizations Area

This project contains several premade modules for you to implement your code. We hope this helps you with ´what to put
where´.

### API

This module is where you have to implement the API interface and connect the other two modules

### Data

This module is where you implement all stateful Mongo data. We have provided an embedded Mongo configuration for you.
You just need to design the data you need to store and the repositories to store or retrieve it with.

### Domain

This module represents the domain you will be working with. The domain module presents classes for the power of attorney
model that contains a Read or Write authorization for a Payment or Savings account.

## The task at hand

Implement the following business requirement

- Users must be able to create write or read access for payments and savings accounts
- Users need to be able to retrieve a list of accounts they have read or write access for

Boundaries

- You can add dependencies as you like
- You can design the data and API models as you like (what a dream, isn't it?)

Notes

- The code should be ready to go to production on delivery

## Background information

A Power of Attorney is used when someone (grantor) wants to give access to his/her account to someone else (grantee). This
could be read access or write access. In this way the grantee can read/write in the grantors account.
Notice that this is a simplified version of reality.
