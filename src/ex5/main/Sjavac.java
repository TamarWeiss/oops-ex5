package ex5.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Sjavac {
    private static final int SUCCESS = 0;
    private static final int FAILURE = 1;
    private static final int ERROR = 2;

    //name pending
    private static int read(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java ex5.main.Sjavac <source_file_name>");
            return ERROR;
        }

        String filename = args[0], line;
        try (BufferedReader buffer = new BufferedReader(new FileReader(filename))) {
            while ((line = buffer.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.err.println("File not found: " + filename);
            return ERROR;
        }

        return SUCCESS;
    }

    public static void main(String[] args) {
        System.out.println(read(args));
    }
}
