package io.muic.kiselevart.ssc;

import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import io.muzoo.ssc.assignment.tracker.SscAssignment;
import org.apache.commons.cli.*;
import java.util.*;

public class Main extends SscAssignment {
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("c", "count-duplicates", false, "prints the total count of duplicate files");
        options.addOption("a", "algorithm", true, "specifies the algorithm used");
        options.addOption("p", "print", false, "prints relative paths of all duplicates grouped together");
        options.addOption("f", true, "specifies path to folder, must be provided.");

        List<String> validAlgorithms = Arrays.asList("bbb", "sha256", "md5");
        
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String pathArg = cmd.getOptionValue("f");

            if (pathArg == null) {
                throw new ParseException("File path -f is required.");
            }

            Path path = Paths.get(pathArg);

            String algorithm = cmd.getOptionValue("a", "sha256"); //defaults to sha256
            if (!validAlgorithms.contains(algorithm)) {
                throw new NoSuchAlgorithmException("Invalid Algorithm");
            }

            boolean printCount = cmd.hasOption("c");
            boolean printPaths = cmd.hasOption("p");

            ChecksumCalculator checksumCalculator = new ChecksumCalculator(algorithm);
            DuplicateFinder duplicateFinder = new DuplicateFinder(checksumCalculator);

            FileCounter.countFiles(path);
            duplicateFinder.countDuplicates(path, printCount, printPaths);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("dirwalker", options);
        }
    }
}