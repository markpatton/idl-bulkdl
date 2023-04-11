package edu.ucsf.library.idl.bulkdl;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;

public class ZipUploadEventHandler implements RequestHandler<S3Event, Object> {

    @Override
    public Object handleRequest(S3Event event, Context context) {
        LambdaLogger logger = context.getLogger();
        S3EventNotificationRecord record = event.getRecords().get(0);

        logger.log("Event: " + record.getEventName());

        return null;
    }

}
