package test.dou.testdrive.config;

/**
 * 测试相关配置常量。
 */
public final class TestConfig {
    private TestConfig() {
    }

    /** 单 APK：instrumentation 声明在主 manifest，am instrument 以主包名为组件。
     *  targetPackage 指向 com.hmdglobal.app.activation（独立 uid），用例崩溃不影响主 UI 进程。 */
    public static final String INSTRUMENTATION_TARGET = "test.dou.testdrive";

    /** instrumentation targetPackage：独立 uid 的已装应用，am instrument 只 force-stop 此进程 */
    public static final String INSTRUMENTATION_TARGET_PACKAGE = "com.hmdglobal.app.activation";

    /** instrumentation runner 类全名 */
    public static final String RUNNER_CLASS = "androidx.test.runner.AndroidJUnitRunner";

    /** 结果日志落盘目录（工厂/离线读取用） */
    public static final String LOG_DIR = "/sdcard/TestDrive/log";

    /** DoU 循环测试报告目录：每天(轮)一个 CSV，记录每个场景的开始/结束电量 */
    public static final String DOU_REPORT_DIR = "/sdcard/DOUreport";
}