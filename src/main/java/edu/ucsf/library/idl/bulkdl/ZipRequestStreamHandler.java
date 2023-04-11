package edu.ucsf.library.idl.bulkdl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.services.s3.S3Client;

public class ZipRequestStreamHandler implements RequestStreamHandler {
    private final S3Client s3_client;
    private final String bucket;

    public ZipRequestStreamHandler() {
        this.s3_client = Config.s3Client();
        this.bucket = Config.sourceBucket();
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        JsonNode node = JsonNodeParser.create().parse(input);

        List<String> keys = node.asArray().stream().map(JsonNode::asString).collect(Collectors.toList());

        Util.zip(s3_client, bucket, keys, Base64.getEncoder().wrap(output));
    }
}
