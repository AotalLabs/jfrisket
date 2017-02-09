package com.aotal.frisket.component;

import com.aotal.frisket.service.ConversionService;
import com.aotal.frisket.service.DecompressService;
import com.aotal.frisket.service.QueueService;
import com.aotal.frisket.service.StorageService;
import org.apache.commons.compress.archivers.ArchiveException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by allan on 9/02/17.
 */
@Component
public class Task {

    private final QueueService queueService;
    private final StorageService storageService;
    private final DecompressService decompressService;
    private final ConversionService conversionService;

    @Inject
    public Task(QueueService queueService, StorageService storageService, DecompressService decompressService, ConversionService conversionService) {
        this.queueService = queueService;
        this.storageService = storageService;
        this.decompressService = decompressService;
        this.conversionService = conversionService;
    }

    @Scheduled(fixedDelay = 1)
    public void doTask() throws InterruptedException, IOException, ArchiveException {
        String filename = queueService.getMessage();
        if (filename == null) {
            Thread.sleep(1000);
            return;
        }
        if (!storageService.documentPending(filename)) {
            Thread.sleep(1000);
            return;
        }
        InputStream stream = storageService.getDocument(filename);

        // Java does not handle directories incredibly well and there are too many error cases to manually delete the created directories
        // If an exception ever occurs then there will be stale records and the container will start to bloat
        Path processingDir = Files.createTempDirectory("processing");
        Path processedDir = Files.createTempDirectory("processed");

        decompressService.decompress(stream, processingDir);

        conversionService.convert(processingDir, processedDir, filename);

        Path converted = processedDir.resolve(filename + ".pdf");
        storageService.uploadDocument(filename + ".pdf", Files.newInputStream(converted));

        Files.delete(processingDir);
        Files.delete(processedDir);
    }
}
