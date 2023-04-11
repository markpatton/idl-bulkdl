package edu.ucsf.library.idl.bulkdl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class Util {
    public static void zip(S3Client s3_client, String bucket, List<String> keys, OutputStream os) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(os);

        for (String key : keys) {
            GetObjectRequest req = GetObjectRequest.builder().bucket(bucket).key(key).build();

            try (ResponseInputStream<GetObjectResponse> resp = s3_client.getObject(req)) {
                ZipEntry entry = new ZipEntry(key);
                zos.putNextEntry(entry);
                resp.transferTo(zos);
                zos.closeEntry();
            }
        }
    }
}
