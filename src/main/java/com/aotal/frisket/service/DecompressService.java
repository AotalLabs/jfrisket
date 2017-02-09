package com.aotal.frisket.service;

import org.apache.commons.compress.archivers.ArchiveException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Created by allan on 9/02/17.
 */
public interface DecompressService {

    /**
     * Decompresses the input stream into the given output directory
     *
     * @param stream
     * @param outdir
     */
    void decompress(InputStream stream, Path outdir) throws IOException, ArchiveException;
}
