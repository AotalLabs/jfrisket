package com.aotal.frisket.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by allan on 9/02/17.
 */
@Service
public class SQSService implements QueueService {

    private final String queueName;
    private final AmazonSQS amazonSqs;
    private final Tracer tracer;

    @Inject
    public SQSService(@Value("#{environment.AWS_SERVICE_PREFIX}") String queueName, AmazonSQS amazonSqs, Tracer tracer) {
        this.queueName = queueName;
        this.amazonSqs = amazonSqs;
        this.tracer = tracer;
    }

    @Override
    public String getMessage() {
        Span sp = tracer.createSpan("Poll Queue");
        try {
            Span getQueueSp = tracer.createSpan("GetQueueUrl", sp);
            String queue;
            try {
                queue = amazonSqs.getQueueUrl(queueName).getQueueUrl();
            } finally {
                tracer.close(getQueueSp);
            }
            ReceiveMessageRequest request = new ReceiveMessageRequest().withQueueUrl(queue).withMaxNumberOfMessages(1);
            Span receiveMessageSp = tracer.createSpan("ReceiveMessage", sp);
            List<Message> messages;
            try {
                messages = amazonSqs.receiveMessage(request).getMessages();
            } finally {
                tracer.close(receiveMessageSp);
            }
            if (messages.size() == 0) {
                return null;
            }
            Span deleteMessageSp = tracer.createSpan("DeleteMessage", sp);
            try {
                amazonSqs.deleteMessage(queue, messages.get(0).getReceiptHandle());
            } finally {
                tracer.close(deleteMessageSp);
            }
            return messages.get(0).getBody();
        } finally {
            tracer.close(sp);
        }
    }
}
