package com.aotal.frisket.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by allan on 9/02/17.
 */
@Service
public class S3Service implements StorageService {

    private final String PENDING_BUCKET;
    private final String ERROR_BUCKET;
    private final String DONE_BUCKET;

    private final AmazonS3 s3;

    @Inject
    public S3Service(AmazonS3 s3, @Value("#{environment.AWS_SERVICE_PREFIX}") String prefix) {
        this.s3 = s3;
        PENDING_BUCKET = prefix + "-pending";
        ERROR_BUCKET = prefix + "-error";
        DONE_BUCKET = prefix + "-done";
    }

    @Override
    public boolean documentPending(String filename) {
        return s3.doesObjectExist(PENDING_BUCKET, filename);
    }

    @Override
    public void uploadDocument(String filename, File in) throws IOException {
        s3.putObject(DONE_BUCKET, filename, in);
    }

    @Override
    public void uploadError(String filename, String error, int code) throws IOException {
        ObjectMetadata meta = new ObjectMetadata();
        meta.addUserMetadata("Error", error);
        meta.addUserMetadata("Response", "" + code);
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(PENDING_BUCKET, filename, ERROR_BUCKET, filename).withNewObjectMetadata(meta);
        s3.copyObject(copyObjectRequest);
    }

    @Override
    public InputStream getDocument(String filename) {
        return s3.getObject(PENDING_BUCKET, filename).getObjectContent();
    }
}
