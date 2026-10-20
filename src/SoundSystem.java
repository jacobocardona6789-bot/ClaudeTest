import javax.sound.sampled.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Sistema de sonido procedural que sintetiza efectos de sonido retro de 16-bits para Mega Man X.
 */
public class SoundSystem {
    private static final int SAMPLE_RATE = 44100;
    private static final Map<String, Clip> clips = new HashMap<>();
    private static boolean soundEnabled = true;

    public static synchronized void init() {
        try {
            registerSound("lemon", createLemonSound());
            registerSound("charge1", createCharge1Sound());
            registerSound("charge2", createCharge2Sound());
            registerSound("dash", createDashSound());
            registerSound("jump", createJumpSound());
            registerSound("wallkick", createWallKickSound());
            registerSound("hit", createHitSound());
            registerSound("explode", createExplodeSound());
            registerSound("hurt", createHurtSound());
        } catch (Exception e) {
            System.err.println("SoundSystem no disponible: " + e.getMessage());
            soundEnabled = false;
        }
    }

    private static void registerSound(String name, byte[] pcmData) {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                soundEnabled = false;
                return;
            }
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(format, pcmData, 0, pcmData.length);
            clips.put(name, clip);
        } catch (Exception ignored) {}
    }

    public static void play(String name) {
        if (!soundEnabled) return;
        Clip c = clips.get(name);
        if (c != null) {
            if (c.isRunning()) c.stop();
            c.setFramePosition(0);
            c.start();
        }
    }

    // --- Generadores de ondas PCM ---

    private static byte[] createLemonSound() {
        int length = SAMPLE_RATE / 16;
        byte[] data = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double freq = 900.0 * (1.0 - t * 0.7);
            double sample = Math.sin(2 * Math.PI * freq * i / SAMPLE_RATE);
            sample += 0.3 * Math.signum(sample);
            double env = 1.0 - t;
            short val = (short) (sample * env * 14000);
            data[i * 2] = (byte) (val & 0xff);
            data[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return data;
    }

    private static byte[] createCharge1Sound() {
        int length = SAMPLE_RATE / 8;
        byte[] data = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double freq = 600.0 + 300.0 * Math.sin(t * Math.PI * 4);
            double sample = Math.sin(2 * Math.PI * freq * i / SAMPLE_RATE);
            double env = Math.sin(t * Math.PI);
            short val = (short) (sample * env * 16000);
            data[i * 2] = (byte) (val & 0xff);
            data[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return data;
    }

    private static byte[] createCharge2Sound() {
        int length = SAMPLE_RATE / 4;
        byte[] data = new byte[length * 2];
        java.util.Random rnd = new java.util.Random(42);
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double freq = 300.0 * (1.0 - t * 0.5) + (rnd.nextDouble() * 80);
            double tone = Math.sin(2 * Math.PI * freq * i / SAMPLE_RATE);
            double noise = (rnd.nextDouble() * 2.0 - 1.0) * 0.5;
            double sample = (tone * 0.7 + noise * 0.5);
            double env = (t < 0.1) ? (t / 0.1) : (1.0 - (t - 0.1) / 0.9);
            short val = (short) (sample * env * 22000);
            data[i * 2] = (byte) (val & 0xff);
            data[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return data;
    }

    private static byte[] createDashSound() {
        int length = SAMPLE_RATE / 5;
        byte[] data = new byte[length * 2];
        java.util.Random rnd = new java.util.Random(13);
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double noise = rnd.nextDouble() * 2.0 - 1.0;
            double freq = 800.0 * (1.0 - t * 0.6);
            double whoosh = Math.sin(2 * Math.PI * freq * i / SAMPLE_RATE);
            double sample = noise * 0.6 + whoosh * 0.4;
            double env = Math.sin(t * Math.PI);
            short val = (short) (sample * env * 18000);
            data[i * 2] = (byte) (val & 0xff);
            data[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return data;
    }

    private static byte[] createJumpSound() {
        int length = SAMPLE_RATE / 12;
        byte[] data = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double freq = 200.0 + 450.0 * t;
            double sample = Math.sin(2 * Math.PI * freq * i / SAMPLE_RATE);
            double env = 1.0 - t * 0.8;
            short val = (short) (sample * env * 14000);
            data[i * 2] = (byte) (val & 0xff);
            data[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return data;
    }

    private static byte[] createWallKickSound() {
        int length = SAMPLE_RATE / 18;
        byte[] data = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double freq = 350.0 + 300.0 * (1.0 - t);
            double sample = Math.signum(Math.sin(2 * Math.PI * freq * i / SAMPLE_RATE));
            double env = 1.0 - t;
            short val = (short) (sample * env * 15000);
            data[i * 2] = (byte) (val & 0xff);
            data[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return data;
    }

    private static byte[] createHitSound() {
        int length = SAMPLE_RATE / 25;
        byte[] data = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double sample = Math.sin(2 * Math.PI * 1100 * i / SAMPLE_RATE);
            double env = 1.0 - t;
            short val = (short) (sample * env * 16000);
            data[i * 2] = (byte) (val & 0xff);
            data[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return data;
    }

    private static byte[] createExplodeSound() {
        int length = SAMPLE_RATE / 4;
        byte[] data = new byte[length * 2];
        java.util.Random rnd = new java.util.Random(99);
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double noise = rnd.nextDouble() * 2.0 - 1.0;
            double lowRumble = Math.sin(2 * Math.PI * 90 * i / SAMPLE_RATE);
            double sample = noise * 0.7 + lowRumble * 0.5;
            double env = 1.0 - t;
            short val = (short) (sample * env * 20000);
            data[i * 2] = (byte) (val & 0xff);
            data[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return data;
    }

    private static byte[] createHurtSound() {
        int length = SAMPLE_RATE / 8;
        byte[] data = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double freq = 180.0 + (i % 200 > 100 ? 60.0 : -60.0);
            double sample = Math.signum(Math.sin(2 * Math.PI * freq * i / SAMPLE_RATE));
            double env = 1.0 - t;
            short val = (short) (sample * env * 18000);
            data[i * 2] = (byte) (val & 0xff);
            data[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return data;
    }
}
