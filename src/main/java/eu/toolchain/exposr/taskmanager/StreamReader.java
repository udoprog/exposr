package eu.toolchain.exposr.taskmanager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StreamReader extends Thread {
    public static interface Handle {
        public void line(String line);
    }

    final InputStream is;
    final Handle handle;

    public StreamReader(InputStream is, Handle handle) {
        this.is = is;
        this.handle = handle;
    }

    @Override
    public void run() {
        try {
            readLines();
        } catch (IOException e) {
            log.error("Error when consuming stream", e);
        }
    }

    private void readLines() throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is));

        while (true) {
            final String line = reader.readLine();

            if (line == null) {
                break;
            }

            handle.line(line);
        }
    }
}