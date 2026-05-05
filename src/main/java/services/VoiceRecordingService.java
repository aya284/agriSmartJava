package services;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Records microphone PCM to WAV (small helper for messenger voice notes).
 */
public class VoiceRecordingService {

    private static final float SAMPLE_RATE = 44100f;
    private static final int MAX_CAPTURE_BYTES = 10_000_000;

    private TargetDataLine line;
    private Thread worker;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private AudioFormat format;
    private volatile boolean capturing;

    public synchronized void start() throws LineUnavailableException {
        stopLineQuietly();
        buffer.reset();
        format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        capturing = true;
        buffer.reset();
        line.start();
        final TargetDataLine activeLine = line;
        worker = new Thread(() -> {
            byte[] tmp = new byte[4096];
            try {
                while (capturing) {
                    int n = activeLine.read(tmp, 0, tmp.length);
                    if (n > 0) {
                        synchronized (VoiceRecordingService.this) {
                            buffer.write(tmp, 0, n);
                            if (buffer.size() >= MAX_CAPTURE_BYTES) {
                                capturing = false;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                capturing = false;
            }
        }, "agrismart-voice-rec");
        worker.start();
    }

    public synchronized void cancel() {
        capturing = false;
        if (line != null) {
            line.stop();
        }
        joinWorker();
        stopLineQuietly();
        buffer.reset();
    }

    public synchronized void stopAndSaveWav(Path outputWav) throws IOException {
        capturing = false;
        if (line != null) {
            line.stop();
        }
        joinWorker();

        byte[] samples;
        synchronized (this) {
            samples = buffer.toByteArray();
        }
        AudioFormat fmt = format;
        stopLineQuietly();

        if (fmt == null) {
            throw new IOException("Pas d'enregistrement.");
        }
        if (samples.length < fmt.getFrameSize() * 15) {
            throw new IOException("Enregistrement trop court.");
        }
        long frames = samples.length / fmt.getFrameSize();
        try (AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(samples), fmt, frames)) {
            Files.createDirectories(outputWav.getParent());
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputWav.toFile());
        }
        buffer.reset();
    }

    public boolean isCapturing() {
        return capturing;
    }

    private void joinWorker() {
        Thread w = worker;
        if (w == null) {
            return;
        }
        try {
            w.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            worker = null;
        }
    }

    private void stopLineQuietly() {
        if (line != null) {
            try {
                line.stop();
                line.close();
            } catch (Exception ignored) {
            }
            line = null;
        }
    }
}
