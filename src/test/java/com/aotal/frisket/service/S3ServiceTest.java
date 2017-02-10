package com.aotal.frisket.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by allan on 10/02/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class S3ServiceTest {

    private final String PENDING_BUCKET = "null-pending";
    private final String ERROR_BUCKET = "null-error";
    private final String DONE_BUCKET = "null-done";

    @Mock
    private AmazonS3 s3;
    @InjectMocks
    private S3Service service;

    @Test
    public void testDocumentPendingCallsCorrectBucket() {
        service.documentPending(null);
        verify(s3, times(1)).doesObjectExist(PENDING_BUCKET, null);
    }

    @Test
    public void testUploadDocumentCallsCorrectBucket() throws IOException {
        service.uploadDocument(null, null);
        verify(s3, times(1)).putObject(DONE_BUCKET, null, null);
    }

    @Test
    public void testUploadErrorCallsCorrectBucketWithCorrectMetadata() throws IOException {
        ArgumentCaptor<CopyObjectRequest> captor = ArgumentCaptor.forClass(CopyObjectRequest.class);
        service.uploadError(null, "ERROR", 0);
        verify(s3, times(1)).copyObject(captor.capture());
        CopyObjectRequest request = captor.getValue();
        assertThat("Request should be valid value", request, is(not(nullValue())));
        assertThat("Source bucket incorrect", request.getSourceBucketName(), is(PENDING_BUCKET));
        assertThat("Source key incorrect", request.getSourceKey(), is(nullValue()));
        assertThat("Destination bucket incorrect", request.getDestinationBucketName(), is(ERROR_BUCKET));
        assertThat("Destination key incorrect", request.getDestinationKey(), is(nullValue()));
        assertThat("Metadata should not be null", request.getNewObjectMetadata(), is(not(nullValue())));
        assertThat("Metadata error key incorrect", request.getNewObjectMetadata().getUserMetaDataOf("Error"), is("ERROR"));
        assertThat("Metadata response key incorrect", request.getNewObjectMetadata().getUserMetaDataOf("Response"), is("0"));
    }

    @Test
    public void testGetObjectCallsCorrectBucket() {
        given(s3.getObject(PENDING_BUCKET, null)).willReturn(new S3Object());
        assertThat("Should return null value due to stubbing", service.getDocument(null), is(nullValue()));
        verify(s3, times(1)).getObject(PENDING_BUCKET, null);
    }
}
