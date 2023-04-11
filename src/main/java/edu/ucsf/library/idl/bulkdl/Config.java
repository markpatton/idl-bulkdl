
package edu.ucsf.library.idl.bulkdl;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class Config {

    private Config() {
    }

    public static S3Client s3Client() {
        return S3Client.builder().credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.US_WEST_2).httpClientBuilder(UrlConnectionHttpClient.builder()).build();
    }

    private static String get_env(String name) {
        String s = System.getenv(name);

        if (s == null) {
            throw new RuntimeException("Missing required env variable: " + name);
        }

        return s;
    }

    public static String sourceBucket() {
        return get_env("IDL_BULKDL_BUCKET_SOURCE");
    }

    public static String targetBucket() {
        return get_env("IDL_BULKDL_BUCKET_TARGET");
    }
}
