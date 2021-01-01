package de.matthiaseberlein.mp3lib;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FrameTSSE extends Frame {

    private byte encoding;

    /**
     * The 'Software/Hardware and settings used for encoding' frame includes the used audio encoder and its settings when the file was encoded. Hardware refers to hardware encoders, not the computer on which a program was run.
     */
    private String encoder;

    FrameTSSE(String id, int framesize, long startbyte, byte[] flags, File file) {
        super(id, framesize, startbyte, flags, file);

        RandomAccessFile raf;
        BufferedReader br;

        // Open File Reader
        try {
            raf= new RandomAccessFile(file, "r");
            raf.seek(startbyte);

            encoding = raf.readByte();

            if(encoding == 1) {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1));
            } else {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName(encodings[encoding])));
            }

            br.skip(startbyte + 1); //Jump to encoded part

            // Read the whole encoded Part
            CharBuffer buffer = CharBuffer.allocate(framesize-1);
            int bytes_read = br.read(buffer);

            if(bytes_read == 0) {
                System.err.println("No bytes were read.");
            } else if(bytes_read == -1) {
                System.err.println("Error: End of File reached.");
            }
            buffer.position(0);
            encoder = String.valueOf(buffer);




        } catch (FileNotFoundException e) {
            System.out.println("Failed to open File: File not found.");
        } catch (IOException e) {
            System.err.println("Error in TSSE: \n"+e.getMessage());
            e.printStackTrace();
        }


    }

    FrameTSSE(String id, long startbyte, InformationHolder data) {
        super(id, startbyte, data);
        encoding = 0;
        encoder = null;
        setData(data);
        setValidity();
    }

    @Override
    InformationHolder getData() {
        return null;
    }

    @Override
    boolean write(RandomAccessFile raf) throws IOException {
        return false;
    }

    @Override
    byte[] getBytes() {
        return new byte[0];
    }

    @Override
    boolean setData(InformationHolder data) {
        return false;
    }

    @Override
    boolean removeData(InformationHolder data) {
        return false;
    }

    @Override
    boolean addData(InformationHolder data) {
        return false;
    }

    @Override
    String[] getFrameString() {
        return new String[0];
    }

    @Override
    String getIdentifier() {
        return null;
    }

    @Override
    MetaID getPurpose() {
        return null;
    }
}
