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
    private static CsvAppender instance;
    private final ICSVWriter csv;

    public static CsvAppender getInstance(Path path) throws IOException {
        if (instance == null) {   //questo vale solo se utilizzo sempre lo stesso file nel codice
            Files.deleteIfExists(path);
            instance = new CsvAppender(path);

        }
        return instance;
    }

    public CsvAppender(Path path) throws IOException {
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
