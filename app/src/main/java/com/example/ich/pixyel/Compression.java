package com.example.ich.pixyel;

/**
 * Created by Ich on 24.10.2016.
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import android.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 *
 * @author Josua Frank
 */
public class Compression {

    /**
     * Compresses a String using the GZIP algorithm
     *
     * Using:
     * <p>
     * {@code //Compresses the String with gzip}
     * <p>
     * {@code String compressed = compress("Groooooooooooße Nachricht");}
     * <p>
     * <p>
     * {@code //Decompress the String}
     * <p>
     * {@code String decompressed = decompress(compressed);}
     * <p>
     * <p>
     * {@code System.out.println("Dekomprimiert: " + decompressed);}
     * <p>
     * <p>
     * @param toCompress The String to be compressed
     * @return The compressed String
     */
    public static String compress(String toCompress) {
        try {
            Deflater deflater = new Deflater();
            byte[] input = toCompress.getBytes();
            deflater.setInput(input);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length);
            deflater.finish();
            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer); // returns the generated code... index
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
            byte[] output = outputStream.toByteArray();
            return Base64.encodeToString(output,0);
        } catch (IOException ex) {
            System.err.println("Could not compress String: " + ex);
        }
        return "";
    }

    /**
     * Decompresses a String using the GZIP algorithm
     *
     * Using:
     * <p>
     * {@code //Compresses the String with gzip}
     * <p>
     * {@code String compressed = compress("Groooooooooooße Nachricht");}
     * <p>
     * <p>
     * {@code //Decompress the String}
     * <p>
     * {@code String decompressed = decompress(compressed);}
     * <p>
     * <p>
     * {@code System.out.println("Dekomprimiert: " + decompressed);}
     * <p>
     * <p>
     * @param toDecompress The String to be decompressed
     * @return The decompressed String
     */
    public static String decompress(String toDecompress) {
        try {
            Inflater inflater = new Inflater();
            byte[] input = Base64.decode(toDecompress,0);
            inflater.setInput(input);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length);
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
            byte[] output = outputStream.toByteArray();
            return new String(output);
        } catch (IOException | DataFormatException ex) {
            System.err.println("Cou,d not decompress String: " + ex);
        }
        return "";
    }
}