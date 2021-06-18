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

- Logged in as your GitHub user, go to this repository: [aws-codepipeline-java-example](https://github.com/jonh-github/aws-codepipeline-java-example)
- Click on the top right Fork button to fork your own copy under your GitHub account
- git clone the forked copy to your local machine
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

	`docker tag spring-rest-example:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/spring-rest-example:latest`

3. Run the aws ecr get-login-password command:

	```
	aws ecr get-login-password | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com
	```

	'Login Succeeded' should be output.

4. Push the image to Amazon ECR with the repo URI:

	`docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/spring-rest-example:latest`

For further details or if you have issues, see: ECS [docker-basics](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/docker-basics.html).

### Copy `amazoncorretto:11-alpine-jdk` image to ECR

The Dockerfile in this project pulls the `amazoncorretto:11-alpine-jdk` image hosted on Docker Hub.

Unfortunately, Docker Hub have introduced rate limits for anonymous pulls, so the builds will intermittently fail.

See AWS's [Docker Hub rate limit advice](https://aws.amazon.com/blogs/containers/advice-for-customers-dealing-with-docker-hub-rate-limits-and-a-coming-soon-announcement/).

To remedy this, copy the image and push it to ECR:

- `docker pull amazoncorretto:11-alpine-jdk`
- `aws ecr create-repository --repository-name amazoncorretto --region $AWS_DEFAULT_REGION`
- `docker tag amazoncorretto:11-alpine-jdk $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/amazoncorretto:11-alpine-jdk`
- `docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/amazoncorretto:11-alpine-jdk`

## Create ECS Task Definition, Service & Cluster to deploy the docker image from ECR

These steps follow AmazonECS [getting-started-fargate](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/getting-started-fargate.html)
with the specifics to this application given below.

For simplicity use the same name `spring-rest-example` for task, service and cluster name.

### Create Task Definition

- Choose Fargate
- For 'Task Definition Name' use: `spring-rest-example`
- Use your existing ecsTaskExecutionRole
- Task memory: `0.5GB`
- Task CPU: `0.25 vCPU`
- Add Container
	- Container name: `spring-rest-example`
	- image: `$AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/spring-rest-example:latest`
	- Port mappings: `8080`
	- Click Add button
- Click Create button

### Create the Service

#### Configure the Service

- Launch Type: `FARGATE`
- Task Definition - choose previously created task: `spring-rest-example`
- Cluster - create and choose: `spring-rest-example`
- Service name: `spring-rest-example`
- Number of tasks: `1`
- Leave defaults for other options

#### Configure Network

- Cluster VPC - choose your existing VPC (or set one up)
- Subnets - choose your existing Subnet
- Auto-assign public IP: `DISABLED`
- Click Next step button

#### Set Auto Scaling (optional)

Leave as default (first option: Do not adjust the service’s desired count) and click Next step button.

Finally at `Step 4: Review` - review your settings and click Create Service button.

### Configure the Cluster

- Choose `Networking only` which is for Fargate.
- Cluster name: `spring-rest-example`

### Verify the Service is running

Navigate to the `spring-rest-example` ECS service via the web UI.

From Clusters, choose cluster: `spring-rest-example`

In the service table, choose: `spring-rest-example`

Screen breadcrumbs at the top of the page should read:

'Clusters > spring-rest-example > Service: spring-rest-example'

Click on the `Logs` tab and verify you see that Spring Boot has successfully started.

### Update Service's Security Group Inbound rules to allow TCP 8080

The Java application's REST API runs on port: 8080, but the default Inbound rule is port 80, this needs to be changed.

Click on the `Details` tab on the ECS `spring-rest-example` Service.

Click on the Security groups entry link (something like sg-efadae6df4...)

This will open the VPC management console for the security group in a new browser tab.

Click on the Security group ID name and click on the 'Edit inbound rules' button.

Choose type: `Custom TCP` and Port range: `8080`

Click on 'Save rules' button

## AWS CodePipeline setup

Follow the steps below to set up an AWS CodePipeline project for the `spring-rest-example`.

### Create CodePipeline project

From the AWS web console, choose Services -> CodePipeline

Click on the 'Create pipeline' button.

- Pipeline name: `spring-rest-example`
- 'New service role' radio button on
- Click Next button
- For 'Add Source stage' Choose 'Source Provider' `GitHub (Version 2)`
- For documentation read AWS's [connections-github](https://docs.aws.amazon.com/codepipeline/latest/userguide/connections-github.html)
- Click on the 'Connect to GitHub'
- Connect to your forked copy of `aws-codepipeline-java-example`
- Choose branch: `main`
- Click on Next button
- Build provider: choose `AWS CodeBuild`

- Click on 'Create project' button which opens a new browser window in the CodeBuild console - see below

### Create CodeBuild project

- Project name: `spring-rest-example`
- Description: 'CodeBuild of GitHub aws-codepipeline-java-example'
- Managed image - ticked on
- Operating system: `Ubuntu`
- Runtime: `standard`
- Image: `aws/codebuild/standard:4.0`
- Privileged: Click on the tick box to 'Enable this flag if you want to build Docker images or want your builds to get elevated privileges'
- Leave as New service role selected
- Open the Additional configuration section and ensure you add the five Plaintext environment variables given below

#### CodeBuild environment variables

Use your own values for AWS_ACCOUNT_ID and AWS_DEFAULT_REGION.

```
AWS_ACCOUNT_ID=123456789012
AWS_DEFAULT_REGION=us-east-1
IMAGE_REPO_NAME=spring-rest-example
IMAGE_TAG=latest
ECS_CONTAINER_NAME=spring-rest-example
```

- Click on the 'Continue to CodePipeline' button which will close the CodeBuild window
	and take you back to the CodePipeline Add Build stage page.
- Click on the Next button
- On the Add deploy stage, Deploy provider: Amazon ECS
- Cluster name: `spring-rest-example`
- Service name: `spring-rest-example`
- Image definitions file: `imagedefinitions.json`
- Click on Next button
- Review your details and click on the 'Create pipeline' button

This will start your first CodePipeline run of `spring-rest-example` which will fail as some extra ECR permissions need to be added
to your CodeBuild Service role - see below.

### In IAM, Update the CodeBuildServiceRolePolicy with ECR additions

- Open the AWS IAM service and click on Roles on the left.
- Narrow the search with filter: `codebuild`
- Choose your service role, probably: `codebuild-spring-rest-example-service-role`
- Click on the attached policy, eg. `CodeBuildBasePolicy-spring-rest-example-us-east-1`
- Click on the 'Edit policy' button and choose the JSON tab
- Add the extra ECR permissions given in [AWSCodeBuildServiceRolePolicy_ECR_Additions.json](AWSCodeBuildServiceRolePolicy_ECR_Additions.json)
- Review and save the policy

Go back to AWS Codepipeline and choose your `spring-rest-example` pipeline.

Click on retry of the Build stage and the pipeline should succeed.

## AWS's buildspec.yml

The configuration of AWS CodeBuild is detailed in the [buildspec.yml](buildspec.yml) in the source repository.

For further details see AWS's [build-spec-ref](https://docs.aws.amazon.com/codebuild/latest/userguide/build-spec-ref.html)
