package com.aotal.frisket.service;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/**
 * Created by allan on 9/02/17.
 */
@Service
public class TarService implements DecompressService {

    @Override
    public void decompress(InputStream stream, Path outdir) throws IOException, ArchiveException {
        try (TarArchiveInputStream debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", new GZIPInputStream(stream))) {
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) debInputStream.getNextEntry()) != null) {
                Path outputFile = outdir.resolve(entry.getName());
                if (!entry.isDirectory()) {
                    final OutputStream outputFileStream = Files.newOutputStream(outputFile);
                    IOUtils.copy(debInputStream, outputFileStream);
                    outputFileStream.close();
                }
            }
        }
    }
}
