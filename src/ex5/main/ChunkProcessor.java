package ex5.main;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ChunkProcessor {
    private static final String CHUNK_START = "//!START_CHUNK: ";
    private static final String CHUNK_END = "//!END_CHUNK";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java ChunkProcessor <test_file.sjava>");
            System.exit(1);
        }

        try {
            // Read the full test file
            List<String> lines = Files.readAllLines(Paths.get(args[0]));

            // Process chunks
            int chunkNumber = 0;
            List<String> currentChunk = new ArrayList<>();
            String chunkName = "";
            boolean inChunk = false;

            for (String line : lines) {
                if (line.startsWith(CHUNK_START)) {
                    inChunk = true;
                    chunkNumber++;
                    chunkName = line.substring(CHUNK_START.length()).trim();
                    currentChunk.clear();
                    continue;
                }

                if (line.equals(CHUNK_END)) {
                    if (inChunk) {
                        // Create and test this chunk
                        String fileName = String.format("chunk_%02d_%s.sjava",
                                chunkNumber, chunkName.toLowerCase().replace(' ', '_'));
                        writeChunk(fileName, currentChunk);
                        testChunk(fileName);
                        inChunk = false;
                    }
                    continue;
                }

                if (inChunk) {
                    currentChunk.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void writeChunk(String fileName, List<String> lines) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            for (String line : lines) {
                writer.println(line);
            }
        }
        System.out.println("\nCreated test file: " + fileName);
        System.out.println("Content:");
        lines.forEach(System.out::println);
        System.out.println("----------------------------------------");
    }

    private static void testChunk(String fileName) {
        try {
            // Instead of running as a separate process, call Sjavac directly
            Sjavac.main(new String[]{fileName});
            System.out.println("Test completed for: " + fileName);
        } catch (Exception e) {
            System.err.println("Error testing chunk " + fileName + ": " + e.getMessage());
        }
    }
}