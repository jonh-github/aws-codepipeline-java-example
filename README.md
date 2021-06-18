#  Spring Boot RESTful API MicroService example

## Summary

An example spring-boot reactive(WebFlux) microservice example which provides a simple RESTful API.
The RESTful API is to a single resource: CatalogueItem and supports:

```
List all           - GET http://localhost:8080/api/v1/
Get with id: 1     - GET http://localhost:8080/api/v1/1
Create             - POST http://localhost:8080/api/v1/
Update with id: 1  - PUT http://localhost:8080/api/v1/1
Delete with id: 1  - DELETE http://localhost:8080/api/v1/1
```

An example JSON response of a CatalogueItem:

```
{
		"id": 1001,
		"name": "The Avengers",
		"description": "Marvel's The Avengers Movie",
		"category": "Movies",
		"price": 0.0,
		"inventory": 0,
		"createdOn": "2021-06-18T14:53:29.541585Z",
		"updatedOn": null
}
```

An AWS CodePipeline compiles, tests, docker images the code and deploys it on AWS ECS.

For AWS setup see: [README_AWS.md](README_AWS.md)

## Design

The application uses spring-boot [reactive](https://www.reactivemanifesto.org/)
[WebFlux](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
and [Spring Data R2DBC](https://spring.io/projects/spring-data-r2dbc) to reduce bespoke application code to a minimum.

An in-memory demo H2 database provides the RDBMS.

[Project Lombok](https://projectlombok.org/) is also used to reduce Java boiler-plate code.

The code is:

```
REST API            -> Service              -> Persistence
CatalogueController -> CatalogueCrudService -> CatalogueRepository
```

[Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html) is included which gives a RESTful API on info, health and metrics.

## Development

### Prerequisites

Install the following:

- [Docker Desktop](https://www.docker.com/get-started)
- [aws cli](https://aws.amazon.com/cli/)
- The OpenJDK 11 (LTS) from [AdoptOpenJDK](https://adoptopenjdk.net/)

### Build and Test

The project comes with [mvnw](https://www.baeldung.com/maven-wrapper) to run `Apache Maven 3.8.1`, so you do not need to install mvn.

In a terminal window in the project directory, to build just run:

`./mvnw clean install`

### Docker build and run

- Build a docker image using the Dockerfile: `docker build --tag=spring-rest-example:latest .`
- Run the docker image: `docker run -it -p8080:8080 spring-rest-example:latest`
- Test the application docker image, GET `http://localhost:8080/actuator/health` should return `{"status":"UP"}`

### Postman collection and environment to test running docker image

Get [Postman](https://www.postman.com/product/rest-client/) and install it.

Import the project's Postman collection: `spring-rest-example.postman_collection.json`
and environment: `spring-rest-example-localhost.postman_environment.json`, to test the running docker image.
