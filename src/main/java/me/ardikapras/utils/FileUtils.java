package me.ardikapras.utils;

import lombok.NoArgsConstructor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class FileUtils {
    public static File unzipToTemp(File gzipFile) throws IOException {
        Path tempDir = Files.createTempDirectory("xml_benchmark_");
        File unzippedFile = new File(tempDir.toFile(), "unzipped.xml");

        try (FileInputStream fis = new FileInputStream(gzipFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GZIPInputStream gzis = new GZIPInputStream(bis);
             FileOutputStream fos = new FileOutputStream(unzippedFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            bos.flush();
        }
        return unzippedFile;
    }

    public static void deleteQuietly(File file) {
        try {
            if (file != null) {
                Files.deleteIfExists(file.toPath());
            }
        } catch (IOException e) {
            // Ignore
        }
    }
}
