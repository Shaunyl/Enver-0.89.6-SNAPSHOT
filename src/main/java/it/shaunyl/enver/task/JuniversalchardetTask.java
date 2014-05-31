package it.shaunyl.enver.task;

import it.shaunyl.enver.EncodingEvaluation;
import it.shaunyl.enver.exception.TaskException;
import it.shaunyl.enver.exception.UnexpectedEnverException;
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import org.mozilla.universalchardet.UniversalDetector;

public class JuniversalchardetTask extends EncodingEvaluation.Task {

    protected UniversalDetector detector = new UniversalDetector(null);

    protected FileInputStream fis;

    private List<String> listOfFiles = new ArrayList<String>();

    JuniversalchardetTask(EncodingEvaluation.TaskOptions options, EncodingEvaluation.Status status) {
        super(options, status);
    }

    @Override
    public void taskSetup() {
        final String directory = options.getDirectory();

        final String filename = options.getFilename();
        if (filename != null) {
            if (!directory.isEmpty()) {
                listOfFiles.add(filename);
            }
        } else if (directory != null | !directory.isEmpty()) {
            File folder = new File(directory);
            File[] ds = folder.listFiles();

            for (final File file : ds) {
                if (file.isDirectory()) {
                    continue;
                }
                listOfFiles.add(file.getAbsolutePath());
                status.printReportLine(System.out, String.format("File '%s' added to the queue.", file.getName()));
            }
            this.setCycle(listOfFiles.size());
        }
    }

    @Override
    public void taskAtomic(final int i) throws SQLException, TaskException {
        final String currentFile = listOfFiles.get(i);
        try {
            fis = new FileInputStream(currentFile);
        } catch (FileNotFoundException e) {
            Report.appendlnformat("File '%s' does not exists.", currentFile);
            throw new UnexpectedEnverException("" + e.getMessage());
        }

        byte[] buf = new byte[4096];

        int nread;
        try {
            while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
        } catch (IOException ex) {
            throw new UnexpectedEnverException("" + ex.getMessage());
        }

        detector.dataEnd();

        boolean detect = false;
        String encoding = detector.getDetectedCharset();
        if (encoding != null) {
            Report.appendlnformat("Detected encoding = %s for file '%s'", encoding, currentFile);
            if (encoding.equals("UTF-8")) {
                detect = true;
            }
        } else {
            Report.appendlnformat("No encoding detected for file '%s'", currentFile);
            File file = new File(currentFile);
            int count = this.countCharsBuffer(file);
            long fileLength = file.length();
            Report.appendlnformat("Characters in the file: %d, size in bytes of file: %d.", count, fileLength);
            if (fileLength == count) {
                detect = true;
                Report.appendln("Character set could be UTF-8, but there is too little information to find out that.");
            }
        }
        detector.reset();
        if (detect) {
        }
    }

    int countCharsBuffer(File f) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            int charCount = 0;
            char[] cbuf = new char[1024];
            int read = 0;
            while ((read = reader.read(cbuf)) > -1) {
                charCount += read;
            }
            reader.close();
            return charCount;
        } catch (FileNotFoundException ex) {
            throw new UnexpectedEnverException("" + ex.getMessage());
        } catch (IOException ex) {
            throw new UnexpectedEnverException("" + ex.getMessage());
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                throw new UnexpectedEnverException("" + ex.getMessage());
            }
        }
    }
}