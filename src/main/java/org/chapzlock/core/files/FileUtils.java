package org.chapzlock.core.files;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileUtils {

    /**
     * Loads a content of a file to String from the resources directory
     *
     * @param resourcePath path of the file relative to the resources directory
     * @return file content as string
     */
    public static String loadFileAsString(String resourcePath) {
        StringBuilder builder = new StringBuilder();

        try (
            InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath)
        ) {
            assert in != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))
            ) {

                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read file! FileUtils name: " + resourcePath
                + System.lineSeparator() + ex.getMessage(), ex);
        }

        return builder.toString();
    }


}
