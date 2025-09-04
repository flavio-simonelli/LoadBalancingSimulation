package it.pmcsn.lbsim.utils.csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

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
            csv.writeNext(header, false);  // scrive l'intestazione
        }
    }

    public void writeRow(String... values) {
        csv.writeNext(values, false);
    }

    @Override
    public void close() {
        try {
            csv.close();
        } catch (Exception e) {
            e.printStackTrace();   // stampa stacktrace completo con la causa
            throw new RuntimeException("Errore nella chiusura del CSV", e);
        }
    }

    /**
     * Crea un CSV aggregato con i dati ResponseTime da tutti i file Welford nella directory output/csv/
     *
     * @param outputPath percorso dove salvare il CSV aggregato
     * @param csvDirectory directory contenente i file CSV (default: "output/csv/")
     * @throws IOException se ci sono errori nella lettura/scrittura dei file
     */
    public static void createWelfordAggregatedCsv(Path outputPath, String csvDirectory, String type) throws IOException {
        Path csvDir = Paths.get(csvDirectory);

        if (!Files.exists(csvDir)) {
            throw new IOException("Directory non trovata: " + csvDirectory);
        }

        try (CsvAppender aggregatedCsv = new CsvAppender(outputPath,
                "replica_name", "jobs_count", "mean_value")) {

            // Scansiona tutti i file che iniziano con "Welford" nella directory
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(csvDir, "Welford*.csv")) {
                for (Path welfordFile : stream) {
                    processWelfordFile(welfordFile, aggregatedCsv, type);
                }
            }
        }
    }

    /**
     * Versione semplificata che usa la directory di default "output/csv/"
     */
    public static void createWelfordAggregatedCsv(Path outputPath, String type) throws IOException {
        createWelfordAggregatedCsv(outputPath, "output/csv/", type);
    }

    /**
     * Processa un singolo file Welford e estrae i dati ResponseTime
     */
    private static void processWelfordFile(Path welfordFile, CsvAppender aggregatedCsv, String type) throws IOException {
        String replicaName = welfordFile.getFileName().toString().replace(".csv", "");

        try (Reader reader = Files.newBufferedReader(welfordFile, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(reader).build()) {

            String[] headers = csvReader.readNext();
            if (headers == null) {
                System.err.println("File vuoto o non valido: " + welfordFile);
                return;
            }

            // Trova gli indici delle colonne necessarie
            int typeIndex = findColumnIndex(headers, "Type");
            int jobsIndex = findColumnIndex(headers, "Jobs"); // o "jobs_count", "N", etc.
            int meanIndex = findColumnIndex(headers, "Mean"); // o "mean", "average", etc.

            if (typeIndex == -1) {
                System.err.println("Colonna 'Type' non trovata in: " + welfordFile);
                return;
            }

            if (jobsIndex == -1 || meanIndex == -1) {
                System.err.println("Colonne necessarie non trovate in: " + welfordFile +
                        " (jobs: " + jobsIndex + ", mean: " + meanIndex + ")");
                return;
            }

            // Leggi tutte le righe e filtra per Type == "ResponseTime"
            String[] row;
            while ((row = csvReader.readNext()) != null) {
                if (row.length > typeIndex && type.equals(row[typeIndex].trim())) {
                    try {
                        String jobsCount = row.length > jobsIndex ? row[jobsIndex].trim() : "0";
                        String meanValue = row.length > meanIndex ? row[meanIndex].trim() : "0.0";

                        // Valida che i valori siano numerici
                        Double.parseDouble(meanValue);
                        Integer.parseInt(jobsCount);

                        aggregatedCsv.writeRow(replicaName, jobsCount, meanValue);
                    } catch (NumberFormatException e) {
                        System.err.println("Valori non numerici validi in: " + welfordFile +
                                " riga: " + String.join(",", row));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Errore nel processare file: " + welfordFile + " - " + e.getMessage());
        }
    }

    /**
     * Trova l'indice di una colonna dato il nome (case-insensitive)
     */
    private static int findColumnIndex(String[] headers, String... columnNames) {
        for (String columnName : columnNames) {
            for (int i = 0; i < headers.length; i++) {
                if (headers[i] != null && headers[i].trim().equalsIgnoreCase(columnName)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Metodo di utilità per elencare i file Welford trovati (utile per debug)
     */
    public static List<String> listWelfordFiles(String csvDirectory) throws IOException {
        Path csvDir = Paths.get(csvDirectory);
        List<String> welfordFiles = new ArrayList<>();

        if (!Files.exists(csvDir)) {
            return welfordFiles;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(csvDir, "Welford*.csv")) {
            for (Path file : stream) {
                welfordFiles.add(file.getFileName().toString());
            }
        }

        return welfordFiles;
    }
}