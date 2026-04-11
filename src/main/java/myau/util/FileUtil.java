package myau.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileUtil {

    public static String readFile(File file) throws IOException {
        if (!file.exists()) return null;
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    public static void writeFile(File file, String content) throws IOException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.print(content);
        }
    }

    public static void copyFile(File source, File destination) throws IOException {
        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static boolean deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteFile(f);
                }
            }
        }
        return file.delete();
    }

    public static void createDirectoryIfNotExists(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static byte[] readBytes(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    public static void writeBytes(File file, byte[] data) throws IOException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        Files.write(file.toPath(), data);
    }
}
