# IDL Bulk Download

This is an experiment in how to download a set of files from a bucket as a zip using aws lambda functions. A server requests
a bulk download be prepared on behalf of a user. When it is finished the server wants to send email to the user with a link
to download the zip.

A few options are explored. The ZipeRequestStreamHandler takes an array of keys as an argument and streams out a zip.
The ZipUploadRequestHandler takes an array of keys as an argument and zips them up to another bucket. The randomly generated zip
name is returned by the function. Note that both of these functions return results synchonously. There is no real way to perform
operations in the background for a lambda function. The function must join all threads by the time it returns. In addition a
function can run for a maxmimum of 15 minutes. This limits what can be bulk downloaded. Once the function returns, the server
knows the name of the zip and that is is ready and can send email to the user.

This could be done asynchronously by having the list of the requested files be uploaded to a bucket. When a request is uploaded,
then the zip task is started. When the zip task is done the initial request could be updated to indicate it is complete.
(This is still limited to 15 minutes.) The server could poll the uploaded request to figure out when the zip is ready.


TODO:
 * Need to actually test against s3 buckets. Need a test account.
 * At least a little unit testing, integration tests may be too hard
 * Figure out how to deal with expiration. The zips should not stick around forever.
 * Figure out impose limits on bulk downloads to prevent abuse.

# Configuration

Environment variables
* IDL_BULKDL_BUCKET_SOURCE
* IDL_BULKDL_BUCKET_TARGET


# Running locally using docker

Edit the Dockerfile and template.yaml to indicate which RequestHandler will be run.

```
mvn clean compile dependency:copy-dependencies -DincludeScope=runtime
docker build -t idl-bulkdl:test .
docker run -p 9000:8080  idl-bulkdl:test
```

Invoke the function:
```
curl -XPOST "http://localhost:9000/2015-03-31/functions/function/invocations" -d '{}'
```


# Documentation provided by archetype. Clean up later.

## Prerequisites
- Java 1.8+
- Apache Maven
- [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
- Docker

## Development

The generated function handler class just returns the input. The configured AWS Java SDK client is created in `DependencyFactory` class and you can 
add the code to interact with the SDK client based on your use case.

#### Building the project
```
mvn clean install
```

#### Testing it locally
```
sam local invoke
```

#### Adding more SDK clients
To add more service clients, you need to add the specific services modules in `pom.xml` and create the clients in `DependencyFactory` following the same 
pattern as s3Client.

## Deployment

The generated project contains a default [SAM template](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-resource-function.html) file `template.yaml` where you can 
configure different properties of your lambda function such as memory size and timeout. You might also need to add specific policies to the lambda function
so that it can access other AWS resources.

To deploy the application, you can run the following command:

```
sam deploy --guided
```

See [Deploying Serverless Applications](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-deploying.html) for more info.


