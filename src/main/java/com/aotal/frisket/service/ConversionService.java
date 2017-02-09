package com.aotal.frisket.service;

import org.springframework.cloud.sleuth.Span;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by allan on 9/02/17.
 */
public interface ConversionService {

    /**
     * Converts all files within the from directory to PDF format and stores the result in the to directory
     *
     * @param span
     * @param from
     * @param to
     * @param filename
     */
    void convert(Span span, Path from, Path to, String filename) throws IOException;
}
