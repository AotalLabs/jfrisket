package com.aotal.frisket.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Created by allan on 10/02/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class SQSServiceTest {

    @Mock
    private AmazonSQS sqs;
    @Mock
    private Tracer tracer;

    @InjectMocks
    private SQSService service;

    @Test
    public void testGetMessageNoUrl() {
        try {
            service.getMessage();
            fail();
        } catch (NullPointerException e) {
        }
        verify(tracer, times(1)).createSpan("Poll Queue");
        verify(tracer, times(1)).createSpan("GetQueueUrl", (Span) null);
        verify(tracer, times(2)).close(null);
        verify(sqs, times(1)).getQueueUrl((String) null);
        verifyNoMoreInteractions(sqs);
        verifyNoMoreInteractions(tracer);
    }

    @Test
    public void testGetMessageErrorMessages() {
        ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        given(sqs.getQueueUrl((String) null)).willReturn(new GetQueueUrlResult().withQueueUrl("QUEUE"));
        try {
            service.getMessage();
            fail();
        } catch (NullPointerException e) {
        }
        verify(tracer, times(1)).createSpan("Poll Queue");
        verify(tracer, times(1)).createSpan("GetQueueUrl", (Span) null);
        verify(tracer, times(1)).createSpan("ReceiveMessage", (Span) null);
        verify(tracer, times(3)).close(null);
        verify(sqs, times(1)).getQueueUrl((String) null);
        verify(sqs, times(1)).receiveMessage(captor.capture());
        verifyNoMoreInteractions(sqs);
        verifyNoMoreInteractions(tracer);
        ReceiveMessageRequest request = captor.getValue();
        assertThat("Request should have a valid value", request, is(not(nullValue())));
        assertThat("Request queue incorrect", request.getQueueUrl(), is("QUEUE"));
        assertThat("Request max number of messages is incorrect", request.getMaxNumberOfMessages(), is(1));
    }

    @Test
    public void testGetMessageWithNoMessages() {
        ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        given(sqs.getQueueUrl((String) null)).willReturn(new GetQueueUrlResult().withQueueUrl("QUEUE"));
        given(sqs.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(new ReceiveMessageResult().withMessages(Collections.emptyList()));
        assertThat("Should return null value with no messages", service.getMessage(), is(nullValue()));
        verify(tracer, times(1)).createSpan("Poll Queue");
        verify(tracer, times(1)).createSpan("GetQueueUrl", (Span) null);
        verify(tracer, times(1)).createSpan("ReceiveMessage", (Span) null);
        verify(tracer, times(3)).close(null);
        verify(sqs, times(1)).getQueueUrl((String) null);
        verify(sqs, times(1)).receiveMessage(captor.capture());
        verifyNoMoreInteractions(sqs);
        verifyNoMoreInteractions(tracer);
        ReceiveMessageRequest request = captor.getValue();
        assertThat("Request should have a valid value", request, is(not(nullValue())));
        assertThat("Request queue incorrect", request.getQueueUrl(), is("QUEUE"));
        assertThat("Request max number of messages is incorrect", request.getMaxNumberOfMessages(), is(1));
    }

    @Test
    public void testGetMessageWithErrorOnDelete() {
        ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        given(sqs.getQueueUrl((String) null)).willReturn(new GetQueueUrlResult().withQueueUrl("QUEUE"));
        given(sqs.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(new ReceiveMessageResult().withMessages(Collections.singleton(new Message())));
        willThrow(new RuntimeException()).given(sqs).deleteMessage("QUEUE", null);
        try {
            service.getMessage();
            fail();
        } catch (Exception e) {
        }
        verify(tracer, times(1)).createSpan("Poll Queue");
        verify(tracer, times(1)).createSpan("GetQueueUrl", (Span) null);
        verify(tracer, times(1)).createSpan("ReceiveMessage", (Span) null);
        verify(tracer, times(1)).createSpan("DeleteMessage", (Span) null);
        verify(tracer, times(4)).close(null);
        verify(sqs, times(1)).getQueueUrl((String) null);
        verify(sqs, times(1)).receiveMessage(captor.capture());
        verify(sqs, times(1)).deleteMessage("QUEUE", null);
        verifyNoMoreInteractions(sqs);
        verifyNoMoreInteractions(tracer);
        ReceiveMessageRequest request = captor.getValue();
        assertThat("Request should have a valid value", request, is(not(nullValue())));
        assertThat("Request queue incorrect", request.getQueueUrl(), is("QUEUE"));
        assertThat("Request max number of messages is incorrect", request.getMaxNumberOfMessages(), is(1));
    }

    @Test
    public void testGetMessage() {
        ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        given(sqs.getQueueUrl((String) null)).willReturn(new GetQueueUrlResult().withQueueUrl("QUEUE"));
        given(sqs.receiveMessage(any(ReceiveMessageRequest.class))).willReturn(new ReceiveMessageResult().withMessages(Collections.singleton(new Message().withBody("MESSAGE"))));
        assertThat("Should have valid response", service.getMessage(), is("MESSAGE"));
        verify(tracer, times(1)).createSpan("Poll Queue");
        verify(tracer, times(1)).createSpan("GetQueueUrl", (Span) null);
        verify(tracer, times(1)).createSpan("ReceiveMessage", (Span) null);
        verify(tracer, times(1)).createSpan("DeleteMessage", (Span) null);
        verify(tracer, times(4)).close(null);
        verify(sqs, times(1)).getQueueUrl((String) null);
        verify(sqs, times(1)).receiveMessage(captor.capture());
        verify(sqs, times(1)).deleteMessage("QUEUE", null);
        verifyNoMoreInteractions(sqs);
        verifyNoMoreInteractions(tracer);
        ReceiveMessageRequest request = captor.getValue();
        assertThat("Request should have a valid value", request, is(not(nullValue())));
        assertThat("Request queue incorrect", request.getQueueUrl(), is("QUEUE"));
        assertThat("Request max number of messages is incorrect", request.getMaxNumberOfMessages(), is(1));
    }
}
