package com.gatling.openapi.plugin;

import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CsvWriter {

    private final File file;

    public CsvWriter(File file) {
        this.file = file;
    }

    public void writeSchemaData(Schema<?> schema, int rows, SchemaExampleGenerator generator) throws IOException {
        if (schema.getProperties() == null || schema.getProperties().isEmpty()) {
            // Si pas de propriétés, générer une colonne "value"
            writeSingleColumnData(schema, rows, generator);
            return;
        }

        List<String> headers = new ArrayList<>(schema.getProperties().keySet());
        List<Map<String, Object>> data = new ArrayList<>();

        for (int i = 0; i < rows; i++) {
            Object example = generator.generateExample(schema, i);
            if (example instanceof Map) {
                data.add((Map<String, Object>) example);
            }
        }

        writeDataset(data);
    }

    private void writeSingleColumnData(Schema<?> schema, int rows, SchemaExampleGenerator generator) throws IOException {
        try (FileWriter writer = new FileWriter(file);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("value"))) {

            for (int i = 0; i < rows; i++) {
                Object value = generator.generateExample(schema, i);
                csvPrinter.printRecord(value);
            }
        }
    }

    public void writeDataset(List<Map<String, Object>> dataset) throws IOException {
        if (dataset.isEmpty()) {
            return;
        }

        // Extraire les headers de la première ligne
        Set<String> headerSet = new LinkedHashSet<>();
        for (Map<String, Object> row : dataset) {
            headerSet.addAll(row.keySet());
        }

        String[] headers = headerSet.toArray(new String[0]);

        try (FileWriter writer = new FileWriter(file);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers))) {

            for (Map<String, Object> row : dataset) {
                List<Object> values = new ArrayList<>();
                for (String header : headers) {
                    Object value = row.get(header);
                    values.add(value != null ? value : "");
                }
                csvPrinter.printRecord(values);
            }
        }
    }
}
