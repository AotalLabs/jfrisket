package com.aotal.frisket.component;

import com.aotal.frisket.service.ConversionService;
import com.aotal.frisket.service.DecompressService;
import com.aotal.frisket.service.QueueService;
import com.aotal.frisket.service.StorageService;
import org.apache.commons.compress.archivers.ArchiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
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

    private static Logger logger = LoggerFactory.getLogger(Task.class);

    private final QueueService queueService;
    private final StorageService storageService;
    private final DecompressService decompressService;
    private final ConversionService conversionService;
    private final Tracer tracer;

    @Inject
    public Task(QueueService queueService, StorageService storageService, DecompressService decompressService, ConversionService conversionService, Tracer tracer) {
        this.queueService = queueService;
        this.storageService = storageService;
        this.decompressService = decompressService;
        this.conversionService = conversionService;
        this.tracer = tracer;
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
        Span sp = tracer.createSpan("Process Task");
        try {
            InputStream stream;
            Span getDocumentSp = tracer.createSpan("GetObject", sp);
            try {
                stream = storageService.getDocument(filename);
            } catch (Exception e) {
                storageService.uploadError(filename, "Could not find " + filename, 404);
                logger.debug("Could not find " + filename, e);
                return;
            } finally {
                tracer.close(getDocumentSp);
            }
            Path processingDir = null;
            Path processedDir = null;
            try {
                processingDir = Files.createTempDirectory("processing");
                try {
                    processedDir = Files.createTempDirectory("processed");

                    Span decompressSp = tracer.createSpan("Decompressing File", sp);
                    try {
                        decompressService.decompress(stream, processingDir);
                    } catch (IOException e) {
                        storageService.uploadError(filename, "Could not decompress file", 530);
                        logger.debug("Could not decompress file", e);
                        return;
                    } catch (ArchiveException e) {
                        storageService.uploadError(filename, "Could not decompress file", 532);
                        logger.debug("Could not decompress file", e);
                        return;
                    } catch (Exception e) {
                        storageService.uploadError(filename, "Severe decompress error", 500);
                        logger.debug("Severe decompress error", e);
                        return;
                    } finally {
                        tracer.close(decompressSp);
                    }

                    Span conversionSp = tracer.createSpan("Converting Files", sp);
                    try {
                        conversionService.convert(conversionSp, processingDir, processedDir, filename);
                    } catch (IOException e) {
                        storageService.uploadError(filename, "File conversion error", 500);
                        logger.debug("File conversion error", e);
                        return;
                    } catch (Exception e) {
                        storageService.uploadError(filename, "Severe conversion error", 500);
                        logger.debug("Severe conversion error", e);
                        return;
                    } finally {
                        tracer.close(conversionSp);
                    }

                    Path converted = processedDir.resolve(filename + ".pdf");
                    Span putSp = tracer.createSpan("Put Object", sp);
                    try {
                        storageService.uploadDocument(filename + ".pdf", Files.newInputStream(converted));
                    } catch (Exception e) {
                        storageService.uploadError(filename, "Could not upload result", 500);
                        logger.debug("Could not upload result", e);
                    } finally {
                        tracer.close(putSp);
                    }
                } finally {
                    Files.list(processedDir).forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Welp we tried
                        }
                    });
                    Files.delete(processedDir);
                }
            } finally {
                Files.list(processingDir).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Welp we tried
                    }
                });
                Files.delete(processingDir);
            }
        } finally {
            tracer.close(sp);
        }
    }
}
