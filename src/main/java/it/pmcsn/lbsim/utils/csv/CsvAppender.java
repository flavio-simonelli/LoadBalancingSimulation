package it.pmcsn.lbsim.utils.csv;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CsvAppender implements AutoCloseable {
    private final ICSVWriter csv;

    public CsvAppender(Path savingPath, String... header) throws IOException {
        Files.deleteIfExists(savingPath);
        if (savingPath.getParent() != null) {
            Files.createDirectories(savingPath.getParent());
        }
        Writer writer = Files.newBufferedWriter(
                savingPath,
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
