package com.primo.optimizer;

import java.io.*;
import java.util.concurrent.*;

/** Only fixed commands and validated package identifiers reach this boundary. */
final class RootShell {
    static String run(String command) throws Exception {
        Process p = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
        ExecutorService io=Executors.newSingleThreadExecutor();
        Future<String> output=io.submit(() -> {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try (InputStream in=p.getInputStream()) {
                byte[] b=new byte[1024]; int n;
                while ((n=in.read(b)) != -1) { if (bytes.size() < 65536) bytes.write(b,0,n); }
            }
            return bytes.toString("UTF-8").trim();
        });
        try {
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
            while (true) {
                try { int code=p.exitValue(); String result=output.get(2, TimeUnit.SECONDS);
                    if (code != 0) throw new IOException("Command rejected ("+code+"): "+result);
                    if (result.toLowerCase(java.util.Locale.ROOT).contains("error") || result.contains("not supported") || result.contains("not found")) throw new IOException(result);
                    return result;
                } catch (IllegalThreadStateException running) {
                    if (System.nanoTime() >= end) throw new IOException("Root request timed out. Open your root manager and retry.");
                    Thread.sleep(80);
                }
            }
        } finally { p.destroy(); output.cancel(true); io.shutdownNow(); }
    }
}
