package edu.ucsf.library.idl.bulkdl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.awssdk.services.s3.S3Client;

public class ZipUploadRequestHandler implements RequestHandler<List<String>, String> {
    private final S3Client s3_client;
    private final String source_bucket;
    private final String target_bucket;

    public ZipUploadRequestHandler() {
        s3_client = Config.s3Client();
        source_bucket = Config.sourceBucket();
        target_bucket = Config.targetBucket();
    }

    // TODO zip file expiration?

    @Override
    public String handleRequest(List<String> keys, Context context) {
        LambdaLogger logger = context.getLogger();

        logger.log("Request to create zip for :" + keys);

        String key = UUID.randomUUID() + ".zip";

        try (OutputStream os = new S3OutputStream(s3_client, target_bucket, key, 10 * 1024 * 1024)) {
            Util.zip(s3_client, source_bucket, keys, os);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return key;
    }
}
