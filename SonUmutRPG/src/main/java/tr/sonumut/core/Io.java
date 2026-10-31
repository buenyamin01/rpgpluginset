package tr.sonumut.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Tüm dosya yazımları tek bir arka plan iş parçacığında sırayla yapılır. */
public final class Io {
    private static ExecutorService exec;
    private static File logDir;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Io() {}

    public static void start(File dataFolder) {
        logDir = new File(dataFolder, "logs");
        exec = Executors.newSingleThreadExecutor(r -> new Thread(r, "SonUmutRPG-IO"));
    }

    public static void write(File file, String data) {
        if (exec == null || exec.isShutdown()) { doWrite(file, data); return; }
        exec.execute(() -> doWrite(file, data));
    }

    private static void doWrite(File file, String data) {
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            Path tmp = Path.of(file.getPath() + ".tmp");
            Files.writeString(tmp, data, StandardCharsets.UTF_8);
            Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[SonUmutRPG] Dosya yazılamadı: " + file + " -> " + e.getMessage());
        }
    }

    /** logs/<ad>.log dosyasına bir satır ekler. */
    public static void log(String name, String line) {
        String text = "[" + LocalDateTime.now().format(FMT) + "] " + line + System.lineSeparator();
        Runnable r = () -> {
            try {
                logDir.mkdirs();
                Files.writeString(new File(logDir, name + ".log").toPath(), text, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException ignored) { }
        };
        if (exec == null || exec.isShutdown()) r.run(); else exec.execute(r);
    }

    public static void shutdown() {
        if (exec == null) return;
        exec.shutdown();
        try { exec.awaitTermination(15, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
    }
}
