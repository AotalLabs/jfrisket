package com.aotal.frisket.service;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by allan on 9/02/17.
 */
@Service
public class ConversionServiceImpl implements ConversionService {

    private static Logger logger = LoggerFactory.getLogger(ConversionServiceImpl.class);

    private final Tika tika;
    private final Tracer tracer;
    private final StorageService storageService;

    @Inject
    public ConversionServiceImpl(Tika tika, Tracer tracer, StorageService storageService) {
        this.tika = tika;
        this.tracer = tracer;
        this.storageService = storageService;
    }

    @Override
    public void convert(Span span, Path from, Path to, String filename) throws IOException {
        List<String> files = new ArrayList<>();
        Files.list(from).forEach(path -> {
            try {
                // Not guaranteed to work
                final String mime = tika.detect(path.toFile());
                switch (mime) {
                    case "application/pdf":
                        Files.createSymbolicLink(to.resolve(path.getFileName()), path);
                        break;
                    case "text/html":
                    case "text/htm":
                        ProcessBuilder pb = new ProcessBuilder("wkhtmltopdf", "--quiet", "-", to.resolve(path.getFileName()).toString());
                        try {
                            Process p = pb.start();
                            p.getOutputStream().write(Files.readAllBytes(path));
                            p.getOutputStream().close();
                            p.waitFor();
                        } catch (InterruptedException e) {
                            // Deliberaterly left blank
                        }
                        break;
                    default:
                        files.add(path.toString());
                }
            } catch (IOException e) {
                logger.debug("Conversion exception", e);
            }
        });

        if (files.size() > 0) {
            ProcessBuilder dos2unix = new ProcessBuilder(Stream.concat(Stream.of("dos2unix", "--quiet"), files.stream()).collect(Collectors.toList()));
            Span dos2unixSp = tracer.createSpan("Dos2Unix converting", span);
            try {
                dos2unix.start().waitFor();
            } catch (InterruptedException e) {
                // Deliberaterly left blank
            } finally {
                tracer.close(dos2unixSp);
            }
            for (String file : files) {
                ProcessBuilder libre = new ProcessBuilder(Arrays.asList("lowriter", "--invisible", "--convert-to", "pdf:writer_pdf_Export:UTF8", "--outdir", to.toString(), file));
                Span libreSp = tracer.createSpan("Libreoffice Converting", span);
                try {
                    Process p = libre.start();
                    if (!p.waitFor(5, TimeUnit.SECONDS)) {
                        //if (!p.destroyForcibly().waitFor(1, TimeUnit.MINUTES)) {
                            // Something is terribly broken at this stage
                            //storageService.uploadError(filename, "Severe conversion error", 500);
                            //System.exit(1);
                        //}
                        //throw new IOException("Failed to convert " + file);
                    }
                } catch (InterruptedException e) {
                    // Deliberaterly left blank
                } finally {
                    tracer.close(libreSp);
                }
            }

        }
        ProcessBuilder gs = new ProcessBuilder(Stream.concat(Stream.of("gs", "-dBATCH", "-dNOPAUSE", "-dPDFFitPage", "-q", "-sOwnerPassword=reallylongandsecurepassword", "-sDEVICE=pdfwrite", "-sOutputFile=" + to.resolve(filename).toString() + ".pdf"), Files.list(to).map(Path::toString).sorted(String::compareTo)).collect(Collectors.toList()));
        Span gsSp = tracer.createSpan("Stitching", span);
        try {
            gs.start().waitFor();
        } catch (InterruptedException e) {
            // Deliberaterly left blank
        } finally {
            tracer.close(gsSp);
        }
    }
}
