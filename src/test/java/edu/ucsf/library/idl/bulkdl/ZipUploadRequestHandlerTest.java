package edu.ucsf.library.idl.bulkdl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Test the lambda function against s3 buckets. Date is uploaded to the source
 * bucket and written to the target buckets. The test code tries to delete the
 * added objects.
 */
public class ZipUploadRequestHandlerTest {
    private final static S3Client s3_client = Config.s3Client();
    private final static String source_bucket = Config.sourceBucket();
    private final static String target_bucket = Config.targetBucket();
    private final static List<TestFile> test_files = new ArrayList<>();
    private final static Random rand = new Random();

    private static class TestFile implements Comparable<TestFile> {
        byte[] data;
        String name;

        TestFile(String name, int size) {
            this.name = name;
            this.data = new byte[size];
            rand.nextBytes(data);
        }

        public TestFile(String name, byte[] data) {
            this.name = name;
            this.data = data;
        }

        @Override
        public int compareTo(TestFile o) {
            return name.compareTo(o.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TestFile other = (TestFile) obj;
            return Arrays.equals(data, other.data) && Objects.equals(name, other.name);
        }
    }

    // Needs to be more than 5 MB which is the minimum part size for a multipart
    // upload
    private static final int[] test_file_sizes = new int[] { 1000, 10000, 100000, 1 * 1024 * 1024, 5 * 1024 * 1024,
            50000, 124000, 800000, 323000 };

    @BeforeAll
    public static void setup() {
        // Create random test files of the specified sizes

        for (int i = 0; i < test_file_sizes.length; i++) {
            test_files.add(new TestFile("test" + i, test_file_sizes[i]));
        }

        Collections.sort(test_files);

        test_files.forEach(f -> {
            PutObjectRequest req = PutObjectRequest.builder().bucket(source_bucket).key(f.name).build();
            s3_client.putObject(req, RequestBody.fromBytes(f.data));
        });
    }

    @AfterAll
    public static void cleanup() {
        test_files.forEach(f -> {
            DeleteObjectRequest req = DeleteObjectRequest.builder().bucket(source_bucket).key(f.name).build();
            s3_client.deleteObject(req);
        });
    }

    List<TestFile> read_zip(InputStream is) throws IOException {
        List<TestFile> result = new ArrayList<>();

        byte[] buf = new byte[1024 * 1024];

        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                int len = 0;
                while ((len = zis.read(buf)) > 0) {
                    bos.write(buf, 0, len);
                }

                zis.closeEntry();

                result.add(new TestFile(name, bos.toByteArray()));
            }
        }

        Collections.sort(result);
        return result;
    }

    private String write_json_array(List<String> values) {
        JsonWriter jw = JsonWriter.create();
        jw.writeStartArray();

        values.forEach(jw::writeValue);
        jw.writeEndArray();

        return new String(jw.getBytes(), StandardCharsets.UTF_8);
    }

    private String get_request_body() {
        List<String> keys = test_files.stream().map(f -> f.name).collect(Collectors.toList());

        return write_json_array(keys);
    }

    @Test
    public void testHandlRequestMultipartUpload() throws IOException {
        // The buffer size will be set to the 5 MB minimum which will force a multipart
        // upload
        ZipUploadRequestHandler handler = new ZipUploadRequestHandler(100 * 1024);

        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setBody(get_request_body());

        String zip_key = handler.handleRequest(event, null);

        GetObjectRequest req = GetObjectRequest.builder().bucket(target_bucket).key(zip_key).build();
        try (ResponseInputStream<GetObjectResponse> resp = s3_client.getObject(req)) {
            List<TestFile> zipped_files = read_zip(resp);

            assertEquals(test_files.size(), zipped_files.size());
            assertEquals(test_files, zipped_files);
        } finally {
            DeleteObjectRequest delreq = DeleteObjectRequest.builder().bucket(target_bucket).key(zip_key).build();
            s3_client.deleteObject(delreq);
        }
    }

    @Test
    public void testHandlRequestSingleUpload() throws IOException {
        // The buffer size should be set to greater than the size of all the files
        ZipUploadRequestHandler handler = new ZipUploadRequestHandler(20 * 1024 * 1024);

        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setBody(get_request_body());

        String zip_key = handler.handleRequest(event, null);

        GetObjectRequest req = GetObjectRequest.builder().bucket(target_bucket).key(zip_key).build();
        try (ResponseInputStream<GetObjectResponse> resp = s3_client.getObject(req)) {
            List<TestFile> zipped_files = read_zip(resp);

            assertEquals(test_files.size(), zipped_files.size());
            assertEquals(test_files, zipped_files);
        } finally {
            DeleteObjectRequest delreq = DeleteObjectRequest.builder().bucket(target_bucket).key(zip_key).build();
            s3_client.deleteObject(delreq);
        }
    }
}
