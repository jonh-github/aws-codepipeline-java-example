#  AWS CodePipeline from GitHub to ECS setup

## Summary

This guide walks through the setup of an AWS CodePipeline to trigger on a GitHub code commit.

The CodePipeline will:

- Pull the code updates
- Compile and run all the tests through `mvn install`
- Produce Unit test and Code Coverage reports
- Create a docker image containing the freshly built Java jar
- Push the docker image to AWS's ECR tagged with the git commit sha, eg. `d7838a1` and an image tag
- Deploy the new docker image as a rolling update in your existing ECS cluster -> service -> task -> container

The image tag can be used to match GitHub branches to deployments, for example:

- GitHub branch `main` = production - tag is `latest`
- GitHub branch `develop` = develop - tag is `develop`

## Prerequisites

An AWS account with a user (and/or role) setup for development in the AWS web console and command line.
Please refer to [get-set-up-for-amazon-ecs](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/get-set-up-for-amazon-ecs.html)
Ignore the 'Create a key pair' section as this example uses AWS Fargate instead of EC2.
Please read through ECS [getting-started](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/getting-started.html) if you have not used ECS before.

If you have skipped reading the AWS docs, please ensure you have installed:

- [Docker Desktop](https://www.docker.com/get-started)
- [aws cli](https://aws.amazon.com/cli/)

## Initial docker image build and push to ECR

The image name is: `spring-rest-example` which matches the pom.xml's artifactId.

### Local docker image build & test

- Ensure you are on the GitHub branch: `main`
- Build the Java jar: `mvn clean install`
- Build a local docker image using the existing Dockerfile: `docker build --tag=spring-rest-example:latest .`
- Run the docker image: `docker run -it -p8080:8080 spring-rest-example:latest`
- Test the application docker image, GET `http://localhost:8080/actuator/health` should return `{"status":"UP"}`

### Push local docker image to ECR

The following steps use environment variables, eg. `$AWS_DEFAULT_REGION`,
please replace these with actual values, eg. `us-east-1` (or set the variables).

1. Create an AWS ECR repository to store the `spring-rest-example` image with:

	`aws ecr create-repository --repository-name spring-rest-example --region $AWS_DEFAULT_REGION`

	Make a note of your `$AWS_ACCOUNT_ID` which is 12 digits in the JSON output with key: `registryId`

2. Tag the `spring-rest-example` image with the ECR repo URI made up of `$AWS_ACCOUNT_ID` & `$AWS_DEFAULT_REGION`:

	`docker tag spring-rest-example:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/spring-rest-example`

3. Run the aws ecr get-login-password command:

	`aws ecr get-login-password | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com`

	'Login Succeeded' should be output.

4. Push the image to Amazon ECR with the repo URI:

	`docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/spring-rest-example`

For further details or if you have issues, see: [docker-basics](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/docker-basics.html).

## Create ECS Task Definition, Service & Cluster to deploy the docker image from ECR

These steps follow [getting-started-fargate](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/getting-started-fargate.html)
with specifics to this application given below:

### Create Task Definition

### Configure the Service

### Configure the Cluster

### Verify the Service is running

Check the logs

## AWS CodePipeline setup

### Create a GitHub connection

### Create CodePipeline project

### Create CodeBuild project

#### CodeBuild environment variables

`AWS_ACCOUNT_ID=123456789012`
`AWS_DEFAULT_REGION=us-east-1`
`IMAGE_REPO_NAME=spring-rest-example`
`IMAGE_TAG=latest`
`ECS_CONTAINER_NAME=spring-rest-example`

### In AMI, Update the CodeBuildServiceRolePolicy with ECR additions
