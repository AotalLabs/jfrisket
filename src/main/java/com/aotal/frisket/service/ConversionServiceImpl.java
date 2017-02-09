package com.aotal.frisket.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by allan on 9/02/17.
 */
@Service
public class ConversionServiceImpl implements ConversionService {

    private final Tika tika;

    @Inject
    public ConversionServiceImpl(Tika tika) {
        this.tika = tika;
    }

    @Override
    public void convert(Path from, Path to, String filename) throws IOException {
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
                        ProcessBuilder pb = new ProcessBuilder("wkhtmltopdf", "--quiet", path.toString(), to.resolve(path.getFileName()).toString());
                        pb.start();
                        break;
                    default:
                        files.add(path.toString());
                }
            } catch (IOException e) {
                // Can't do anything
            }
        });
        ProcessBuilder dos2unix = new ProcessBuilder(Stream.concat(Stream.of("dos2unix", "--quiet"), files.stream()).collect(Collectors.toList()));
        dos2unix.start();
        ProcessBuilder libre = new ProcessBuilder(Stream.concat(Stream.of("lowriter", "--invisible", "--convert-to", "pdf:writer_pdf_Export:UTF8", "--outdir", "processed"), files.stream()).collect(Collectors.toList()));
        libre.start();
        ProcessBuilder gs = new ProcessBuilder(Stream.concat(Stream.of("gs", "-dBATCH", "-dNOPAUSE", "-dPDFFitPage", "-q", "-sOwnerPassword=reallylongandsecurepassword", "-sDEVICE=pdfwrite", "-sOutputFile=" + to.resolve(filename).toString() + ".pdf"), Files.list(to).map(Path::toString)).collect(Collectors.toList()));
        gs.start();
    }
}
