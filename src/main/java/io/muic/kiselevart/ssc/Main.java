package io.muic.kiselevart.ssc;

import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import io.muzoo.ssc.assignment.tracker.SscAssignment;
import org.apache.commons.cli.*;
import java.util.function.Function;
import java.security.*;

public class Main extends SscAssignment {
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("c", "count-duplicates", false, "prints the total count of duplicate files");
        options.addOption("a", "algorithm", true, "specifies the algorithm used");
        options.addOption("p", "print", false, "prints relative paths of all duplicates grouped together");
        options.addOption("f", true, "specifies path to folder, must be provided.");

        //String.format("%,d", 2000000) for future reference, this is how to comma format numbers

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String pathArg = cmd.getOptionValue("f");

            if (pathArg != null) {
                Path path = Paths.get(pathArg);

                countFiles(path);

                String algorithm = cmd.getOptionValue("a", "sha256"); //by default uses sha256
                Function<Path, String> checksumCalculator;

                switch (algorithm.toLowerCase()) {
                    case "bbb":
                        checksumCalculator = Main::calculateByteChecksum;
                        break;
                    case "md5":
                        checksumCalculator = Main::calculateMD5;
                        break;
                    case "sha256":
                        checksumCalculator = Main::calculateSHA256;
                        break;
                    default:
                        throw new ParseException("Invalid algorithm specified.");
                }

                boolean printPaths = false;
                boolean printCount = false;
                if (cmd.hasOption("c")) {
                    printCount = true;
                }
                if (cmd.hasOption("p")) {
                    printPaths = true;
                }

                countDuplicates(path, checksumCalculator, printCount, printPaths);

            }
            else {
                throw new ParseException("File path -f is required.");
            }

        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("myapp", options);
        }
    }

    private static void countFiles(Path path) {
        final int[] fileCount = new int[1];
        final int[] directoryCount = new int[1];
        final long[] totalSize = new long[1]; // Variable to store total size

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    fileCount[0]++;
                    totalSize[0] += Files.size(file); // Accumulate the size of the file
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    directoryCount[0]++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    System.err.println("Failed to access: " + file.toString() + " due to " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Total number of files is: " + fileCount[0]);
        System.out.println("Total number of directories is: " + directoryCount[0]);
        System.out.println("Total size of all files is: " + totalSize[0] + " bytes");
    }

    private static void countDuplicates(Path path, Function<Path, String> checksumAlgorithm, boolean printCount, boolean printPaths) {
        if (!printCount && !printPaths) {
            return;
        }
        
        Map<String, Integer> checksumMap = new HashMap<>();
        Map<String, List<String>> duplicatePathsMap = new HashMap<>();
    
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String checksum = checksumAlgorithm.apply(file);
                    
                    checksumMap.put(checksum, checksumMap.getOrDefault(checksum, 0) + 1);
                    
                    if (printPaths) {
                        List<String> paths = duplicatePathsMap.computeIfAbsent(checksum, k -> new ArrayList<>());
                        paths.add(file.toString());
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
    
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    System.err.println("Failed to access: " + file.toString() + " due to " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    
        if (printPaths) {
            System.out.println("Duplicate Paths:");
            for (Map.Entry<String, List<String>> entry : duplicatePathsMap.entrySet()) {
                List<String> paths = entry.getValue();
                if (paths.size() > 1) {
                    for (String duplicatePath : paths) {
                        System.out.println(duplicatePath);
                    }
                    System.out.println();
                }
            }
        }
    
        if (printCount) {
            int totalDuplicates = checksumMap.values().stream().mapToInt(count -> count > 1 ? count - 1 : 0).sum();
            System.out.println("The total number of duplicate files is: " + totalDuplicates);
        }
    }    

    private static String calculateByteChecksum(Path file) {
        try {
            byte[] content = Files.readAllBytes(file);
            return Arrays.toString(content);
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.toString() + ". " + e.getMessage());
            throw new RuntimeException("Error reading file: " + file.toString(), e);
        }
    }

    private static String calculateMD5(Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(Files.readAllBytes(file));
            StringBuilder result = new StringBuilder();
            for (byte b : digest) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculating MD5 checksum", e);
        }
    }

    private static String calculateSHA256(Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(Files.readAllBytes(file));
            StringBuilder result = new StringBuilder();
            for (byte b : digest) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculating SHA-256 checksum", e);
        }
    }
}