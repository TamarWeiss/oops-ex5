package ex5.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Sjavac {
    private static final int SUCCESS = 0;
    private static final int FAILURE = 1;
    private static final int ERROR = 2;

    //name pending
    private static int check(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java ex5.main.Sjavac <source_file_name>");
            return ERROR;
        }

        String filename = args[0];
        try {
           Scanner scanner = new Scanner(new File(filename));
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + filename);
            return ERROR;
        }

        return SUCCESS;
    }

    public static void main(String[] args) {
        System.out.println(check(args));
    }
}
