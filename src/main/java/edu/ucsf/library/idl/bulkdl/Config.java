
package edu.ucsf.library.idl.bulkdl;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;

public class Config {

    private Config() {
    }

    public static S3Client s3Client() {
        return S3Client.builder().credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .httpClientBuilder(UrlConnectionHttpClient.builder()).build();
    }

    private static String get_env(String name) {
        String s = System.getenv(name);

        if (s == null) {
            throw new RuntimeException("Missing required env variable: " + name);
        }

        return s;
    }

    private static int get_env_int(String name, int default_value) {
        String s = System.getenv(name);

        if (s == null) {
            return default_value;
        }

        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Unable to parse env variable as integer: " + name, e);
        }
    }

    public static String sourceBucket() {
        return get_env("IDL_BULKDL_BUCKET_SOURCE");
    }

    public static String targetBucket() {
        return get_env("IDL_BULKDL_BUCKET_TARGET");
    }

    public static int bufferSize() {
        return get_env_int("IDL_BULKDL_BUFFER_SIZE", 10 * 1024 * 1024);
    }
}
