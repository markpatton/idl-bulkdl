package edu.ucsf.library.idl.bulkdl;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

/**
 * Adapted from
 * https://gist.github.com/blagerweij/ad1dbb7ee2fff8bcffd372815ad310eb.
 *
 *
 */

public class S3OutputStream extends OutputStream {
    private final S3Client s3_client;
    private final String bucket;
    private final String key;

    // TODO Use ByteBuffer
    private final byte[] buf;
    private int buf_index;
    private boolean is_open;
    private String upload_id;
    private String abort_id;
    private List<CompletedPart> parts;

    public S3OutputStream(S3Client s3_client, String bucket, String key, int buffer_size) {
        this.s3_client = s3_client;
        this.bucket = bucket;
        this.key = key;
        this.buf = new byte[buffer_size];
        this.buf_index = 0;
        this.is_open = true;
        this.upload_id = null;
        this.parts = new ArrayList<>();
    }

    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] byteArray, int offset, int length) {
        assertOpen();

        int size;

        while (length > (size = this.buf.length - buf_index)) {
            System.arraycopy(byteArray, offset, this.buf, this.buf_index, size);
            buf_index += size;

            flush_buffer();

            offset += size;
            length -= size;
        }

        System.arraycopy(byteArray, offset, this.buf, this.buf_index, length);
        buf_index += length;
    }

    @Override
    public synchronized void flush() {
        assertOpen();
    }

    private void flush_buffer() {
        if (upload_id == null) {
            CreateMultipartUploadRequest req = CreateMultipartUploadRequest.builder().bucket(bucket).key(key).build();
            CreateMultipartUploadResponse resp = s3_client.createMultipartUpload(req);

            upload_id = resp.uploadId();
            abort_id = resp.abortRuleId();
        }

        upload_part();
        buf_index = 0;
    }

    private void upload_part() {
        int part = parts.size();
        UploadPartRequest req = UploadPartRequest.builder().bucket(bucket).key(key).partNumber(part).uploadId(upload_id)
                .build();

        ByteArrayInputStream is = new ByteArrayInputStream(buf, 0, buf_index);
        UploadPartResponse resp = s3_client.uploadPart(req, RequestBody.fromInputStream(is, buf_index));

        parts.add(CompletedPart.builder().partNumber(part).eTag(resp.eTag()).build());
    }

    @Override
    public void close() {
        if (this.is_open) {
            this.is_open = false;

            if (this.upload_id != null) {
                if (this.buf_index > 0) {
                    upload_part();
                }

                CompleteMultipartUploadRequest req = CompleteMultipartUploadRequest.builder().bucket(bucket).key(key)
                        .uploadId(upload_id).multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())

                        .build();
                CompleteMultipartUploadResponse resp = s3_client.completeMultipartUpload(req);
            } else {
                PutObjectRequest req = PutObjectRequest.builder().bucket(bucket).key(key).build();
                ByteArrayInputStream is = new ByteArrayInputStream(buf, 0, buf_index);
                PutObjectResponse resp = s3_client.putObject(req, RequestBody.fromInputStream(is, buf_index));
            }
        }
    }

    public void cancel() {
        is_open = false;

        if (upload_id != null) {
            AbortMultipartUploadRequest req = AbortMultipartUploadRequest.builder().bucket(bucket).key(key).uploadId(upload_id).build();
            AbortMultipartUploadResponse resp = s3_client.abortMultipartUpload(req);
        }
    }

    @Override
    public void write(int b) {
        assertOpen();

        if (buf_index >= this.buf.length) {
            flush_buffer();
        }
        this.buf[buf_index++] = (byte) b;
    }

    private void assertOpen() {
        if (!is_open) {
            throw new IllegalStateException("Closed");
        }
    }
}