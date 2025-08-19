package it.pmcsn.lbsim.libs.csv;

import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import it.pmcsn.lbsim.controller.SimulatorController;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

public class CsvAppender implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(CsvAppender.class.getName());
    private final ICSVWriter csv;

    public CsvAppender(Path path, String... header) throws IOException {
        Files.deleteIfExists(path);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Writer writer = Files.newBufferedWriter(
                path,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
        this.csv = new CSVWriterBuilder(writer)
                .withSeparator(',')
                .build();

        if (header != null && header.length > 0) {
            csv.writeNext(header, false);  // scrive l’intestazione
        }
    }

    public void writeRow(String... values) {
        csv.writeNext(values, false);
    }

    @Override
    public void close() {
        try {
            csv.close();
        } catch (IOException e) {
            // ignoriamo o logghiamo se necessario
        }
    }
}
