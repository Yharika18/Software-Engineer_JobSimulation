package com.jpmc.midascore;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FileLoader {

    public String[] loadStrings(String path) {
        try {
            InputStream inputStream = this.getClass().getResourceAsStream(path);

            if (inputStream == null) {
                throw new RuntimeException("File not found: " + path);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            List<String> lines = reader.lines().collect(Collectors.toList());

            return lines.toArray(new String[0]);

        } catch (Exception e) {
            throw new RuntimeException("Error reading file: " + path, e);
        }
    }
}