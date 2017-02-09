package com.aotal.frisket.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import org.springframework.beans.factory.annotation.Value;
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

    @Inject
    public SQSService(@Value("#{environment.AWS_SERVICE_PREFIX}") String queueName, AmazonSQS amazonSqs) {
        this.queueName = queueName;
        this.amazonSqs = amazonSqs;
    }

    @Override
    public String getMessage() {
        String queue = amazonSqs.getQueueUrl(queueName).getQueueUrl();
        ReceiveMessageRequest request = new ReceiveMessageRequest().withQueueUrl(queue).withMaxNumberOfMessages(1);
        List<Message> messages = amazonSqs.receiveMessage(request).getMessages();
        if (messages.size() == 0) {
            return null;
        }
        amazonSqs.deleteMessage(queue, messages.get(0).getReceiptHandle());
        return messages.get(0).getBody();
    }
}
