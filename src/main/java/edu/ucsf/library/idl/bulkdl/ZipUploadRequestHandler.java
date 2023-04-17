package edu.ucsf.library.idl.bulkdl;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Zip objects in a source bucket to a destination object in a target bucket.
 * The client supplies keys of objects in the source bucket and is returned the
 * key of the zip in the target bucket.
 */
public class ZipUploadRequestHandler implements RequestHandler<List<String>, String> {
    private final S3Client s3_client;
    private final String source_bucket;
    private final String target_bucket;
    private final int buffer_size;

    public ZipUploadRequestHandler() {
        this(Config.bufferSize());
    }

    public ZipUploadRequestHandler(int buffer_size) {
        this.s3_client = Config.s3Client();
        this.source_bucket = Config.sourceBucket();
        this.target_bucket = Config.targetBucket();
        this.buffer_size = buffer_size;
    }

    void zip(String bucket, List<String> keys, OutputStream os) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(os)) {
            for (String key : keys) {
                GetObjectRequest req = GetObjectRequest.builder().bucket(bucket).key(key).build();

                try (ResponseInputStream<GetObjectResponse> resp = s3_client.getObject(req)) {
                    zos.putNextEntry(new ZipEntry(key));
                    resp.transferTo(zos);
                    zos.closeEntry();
                }
            }
        }
    }

    @Override
    public String handleRequest(List<String> keys, Context context) {
        String target_key = UUID.randomUUID() + ".zip";

        // Be careful to cancel the upload in case of an exception
        S3OutputStream os = null;

        try {
            os = new S3OutputStream(s3_client, target_bucket, target_key, buffer_size);
            zip(source_bucket, keys, os);
        } catch (IOException e) {
            if (os != null) {
                os.cancel();
            }
            throw new RuntimeException(e);
        } finally {
            if (os != null) {
                os.close();
            }
        }

        return target_key;
    }
}
