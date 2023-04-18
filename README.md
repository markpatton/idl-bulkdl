# IDL Bulk Download

This is a lambda function that takes an array of keys as an argument, zips them together as a single file into another bucket, and returns the randomly generated name of the new file.

# Building

Requirements:
* Java 11+
* Apache Maven
* [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
* Docker


Build with maven:
```
mvn clean install -Dtest.skip
```
The tests need access to s3 buckets. In order to run them you have to do some setup first.

Build and run tests:
```
aws s3 mb s3://test-data
aws s3 mb s3://test-bulkdl

export AWS_ACCESS_KEY_ID=ID
export AWS_SECRET_ACCESS_KEY=KEY
export AWS_REGION=us-east-1

export IDL_BULKDL_BUCKET_SOURCE=oida-test-data
export IDL_BULKDL_BUCKET_TARGET=oida-test-bulkdl

mvn clean install
```

# Configuration

Environment variables
* AWS_ACCESS_KEY_ID
* AWS_SECRET_ACCESS_KEY
* AWS_REGION
* IDL_BULKDL_BUCKET_SOURCE
* IDL_BULKDL_BUCKET_TARGET
* IDL_BULKDL_BUFFER_SIZE

The keys given as an argument to the function refer to objects in the source bucket. The zip file is written to the target bucket.

Note that permissions for the buckets must be set appropriately in template.yaml.

# Manual local testing

Upload some files from a testdata directory to the configured s3 bucket:
```
aws s3 sync testdata/ s3://oida-test-bulkdl/
```

Start the lambda:
```
sam local start-lambda
```

Try to zip up some of the files in the source bucket.
```
curl -XPOST "http://localhost:3001/2015-03-31/functions/bulkdl/invocations" -d '["snhk0078.pdf"]'
```

The name of the zip will be returned.

Retrieve the newly created zip:
```
aws s3 cp s3://oida-test-bulkdl/285f8d27-7f1c-47e3-b88d-90b4dd17d1f6.zip .
```

# Deploy

To deploy the application, you can run the following command:

```
sam deploy --guided
```

See [Deploying Serverless Applications](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-deploying.html) for more info.

