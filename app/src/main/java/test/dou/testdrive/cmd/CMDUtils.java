package test.dou.testdrive.cmd;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * shell 命令执行工具。系统签名后可用 su/系统权限执行相关命令。
 */
public final class CMDUtils {

    private CMDUtils() {
    }

    /** 命令执行结果封装 */
    public static class CMD_Result {
        public String success;
        public String error;
    }

    /**
     * 执行 shell 命令并回读 stdout / stderr。
     *
     * @param command   要执行的命令
     * @param needRoot  是否提升权限到 root（系统签名后通常非必需）
     * @param isExit    执行后是否是否收尾
     */
    public static CMD_Result runCMD(String command, boolean needRoot, boolean isExit) {
        CMD_Result rs = new CMD_Result();
        DataOutputStream os = null;
        try {
            String exec;
            if (needRoot) {
                exec = "su";
            } else {
                exec = "sh";
            }
            Process process = Runtime.getRuntime().exec(exec);
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            if (isExit) {
                os.writeBytes("exit\n");
            }
            os.flush();
            rs.error = (new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))).readLine();
            rs.success = (new BufferedReader(
                    new InputStreamReader(process.getInputStream()))).readLine();
        } catch (IOException e) {
            throw new RuntimeException("执行命令失败: " + command, e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ignore) {
                }
            }
        }
        return rs;
    }

    /**
     * 一种可被可靠中断的命令执行方式：把 stdout/stderr 的读取放到独立守护线程，
     * 主线程通过 {@link #await(java.util.concurrent.atomic.AtomicBoolean)} 轮询等待。
     * 只要收到停止信号，主线程立即返回、不再阻塞——即使底层 read 卡死也能中断，
     * 不依赖 root/pkill 权限或关闭流的可靠性。
     *
     * @param command         要执行的 shell 命令
     * @param stopRequested   停止信号；一旦为 true，{@link #await} 立即返回
     * @return 命令输出（被停止时可能为部分/空输出）
     */
    public static CMD_Result runFull(String command, final java.util.concurrent.atomic.AtomicBoolean stopRequested) {
        try {
            return CommandSession.start(command, stopRequested).await(stopRequested);
        } catch (IOException e) {
            throw new RuntimeException("执行命令失败: " + command, e);
        }
    }

    /**
     * 兼容旧的同步全量读取入口（无停止信号）。
     */
    public static CMD_Result runFull(String command) {
        return runFull(command, new java.util.concurrent.atomic.AtomicBoolean(false));
    }

    /**
     * 命令执行会话：内置守护线程负责异步读取输出，主线程轮询等待并检查停止信号。
     */
    public static class CommandSession {
        private final Process process;
        private final java.util.concurrent.atomic.AtomicReference<CMD_Result> result =
                new java.util.concurrent.atomic.AtomicReference<>();
        private final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);

        /** 启动一个命令会话：写入命令后异步读取输出。 */
        public static CommandSession start(String command, java.util.concurrent.atomic.AtomicBoolean stopRequested)
                throws IOException {
            Process process = Runtime.getRuntime().exec("sh");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            os.close();
            return new CommandSession(process, stopRequested);
        }

        public CommandSession(final Process process, final java.util.concurrent.atomic.AtomicBoolean stopRequested) {
            this.process = process;
            Thread reader = new Thread(() -> {
                try {
                    BufferedReader o = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader e = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    StringBuilder so = new StringBuilder();
                    String line;
                    while ((line = o.readLine()) != null) {
                        so.append(line).append('\n');
                    }
                    StringBuilder se = new StringBuilder();
                    while ((line = e.readLine()) != null) {
                        se.append(line).append('\n');
                    }
                    try {
                        process.waitFor();
                    } catch (InterruptedException ignore) {
                    }
                    CMD_Result r = new CMD_Result();
                    r.success = so.toString();
                    r.error = se.toString();
                    result.set(r);
                    destroy();
                } catch (Exception ignore) {
                } finally {
                    done.countDown();
                }
            }, "cmd-reader");
            reader.setDaemon(true);
            reader.start();
        }

        /**
         * 等待命令结束；每 100ms 检查一次停止信号，收到即返回（即使底层读取仍卡住）。
         */
        public CMD_Result await(boolean stopFlagNow) {
            java.util.concurrent.atomic.AtomicBoolean flag =
                    new java.util.concurrent.atomic.AtomicBoolean(stopFlagNow);
            return await(flag);
        }

        public CMD_Result await(java.util.concurrent.atomic.AtomicBoolean stopRequested) {
            while (!stopRequested.get()) {
                try {
                    if (done.await(100, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                        break;
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
            CMD_Result r = result.get();
            if (r == null) {
                r = new CMD_Result();
                result.set(r);
            }
            destroy();
            return r;
        }

        /** 立即取消：销毁底层 shell 进程并中断读取。 */
        public void cancel() {
            destroy();
        }

        /** 销毁底层 shell 进程，尽可能结束仍在后台运行的命令。 */
        private void destroy() {
            try {
                process.getInputStream().close();
            } catch (Exception ignore) {
            }
            try {
                process.getErrorStream().close();
            } catch (Exception ignore) {
            }
            try {
                process.destroy();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * 强制终止命令行（/proc 下 cmdline，即 pkill -f）匹配 key 的进程。
     * 用于中断阻塞读取的 am instrument 包装进程：销毁 sh 时其子进程 am 仍持有输出管道，
     * 必须杀掉 am 才会让管道的 readLine 返回。
     * 在后台守护线程执行，不阻塞调用者；本 APK 为系统 UID，普通 pkill 即可，必要时回退 su。
     */
    public static void killMatching(final String key) {
        Thread t = new Thread(() -> {
            // 本 APK 为系统 UID，能直接 pkill；失败再回退到 su
            pkill("pkill -9 -f \"" + key + "\"");
            pkill("su -c 'pkill -9 -f \"" + key + "\"'");
        }, "kill-" + key);
        t.setDaemon(true);
        t.start();
    }

    private static void pkill(String cmd) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            // 不等进程自然结束，轮询带超时，避免本身二次阻塞
            for (int i = 0; i < 30; i++) {
                if (!p.isAlive()) {
                    break;
                }
                Thread.sleep(100);
            }
        } catch (Exception ignore) {
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }
}