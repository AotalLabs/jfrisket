package com.aotal.frisket.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by allan on 9/02/17.
 */
public interface StorageService {


    /**
     * Checks that a Document exists in the storage service
     *
     * @param filename
     * @return the downloaded File
     */
    boolean documentPending(String filename);

    /**
     * Uploads the file data to the storage service under the given filename
     *
     * @param filename
     * @param in
     * @throws IOException
     */
    void uploadDocument(String filename, File in) throws IOException;

    /**
     * Uploads the file data to the storage service under the given filename
     *
     * @param filename
     * @param error
     * @param code
     * @throws IOException
     */
    void uploadError(String filename, String error, int code) throws IOException;

    /**
     * Retrieves the specified document from the storage servicea
     *
     * @param filename
     * @return
     */
    InputStream getDocument(String filename);
}
