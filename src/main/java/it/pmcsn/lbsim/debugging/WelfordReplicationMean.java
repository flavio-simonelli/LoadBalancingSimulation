package it.pmcsn.lbsim.debugging;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class WelfordReplicationMean {

    /**
     * Calcola la media dei valori dalla terza riga, terza colonna
     * di tutti i file CSV che iniziano con "welford" nella cartella output/csv
     */
    public double calculateMean() {
        String csvDirectory = "output/csv";
        List<Double> values = new ArrayList<>();

        try {
            // Ottiene il path della directory
            Path dir = Paths.get(csvDirectory);

            // Verifica se la directory esiste
            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                System.err.println("La directory " + csvDirectory + " non esiste o non è una directory.");
                return 0.0;
            }

            // Cerca tutti i file che iniziano con "welford" e finiscono con ".csv"
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "welford*.csv")) {

                for (Path file : stream) {
                    try {
                        double value = extractValueFromCSV(file);
                        values.add(value);
                        System.out.println("File: " + file.getFileName() + " -> Valore: " + value);
                    } catch (Exception e) {
                        System.err.println("Errore nel leggere il file " + file.getFileName() + ": " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Errore nell'accesso alla directory: " + e.getMessage());
            return 0.0;
        }

        if (values.isEmpty()) {
            System.out.println("Nessun file CSV trovato che inizi con 'welford' nella directory " + csvDirectory);
            return 0.0;
        }

        // Calcola la media
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        return sum / values.size();
    }

    /**
     * Estrae il valore dalla terza riga, terza colonna del file CSV
     */
    private double extractValueFromCSV(Path csvFile) throws IOException, NumberFormatException {
        try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
            String line;
            int rowCount = 0;

            // Legge le righe fino alla terza
            while ((line = reader.readLine()) != null) {
                rowCount++;

                if (rowCount == 3) { // Terza riga
                    String[] columns = line.split(",");

                    if (columns.length < 3) {
                        throw new IllegalArgumentException("Il file non ha almeno 3 colonne nella terza riga");
                    }

                    // Rimuove eventuali spazi e virgolette
                    String valueStr = columns[2].trim().replaceAll("\"", "");

                    try {
                        return Double.parseDouble(valueStr);
                    } catch (NumberFormatException e) {
                        throw new NumberFormatException("Impossibile convertire '" + valueStr + "' in numero");
                    }
                }
            }

            throw new IllegalArgumentException("Il file non ha almeno 3 righe");
        }
    }

    public static void main(String[] args) {
        WelfordReplicationMean calculator = new WelfordReplicationMean();

        System.out.println("=== Calcolo della media dei valori Welford ===");
        System.out.println("Directory: output/csv");
        System.out.println("Pattern file: welford*.csv");
        System.out.println("Posizione valore: terza riga, terza colonna");
        System.out.println();

        double mean = calculator.calculateMean();

        System.out.println();
        System.out.println("=== RISULTATO ===");
        System.out.printf("Media calcolata: %.6f%n", mean);
    }
}