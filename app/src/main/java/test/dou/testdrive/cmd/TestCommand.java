package test.dou.testdrive.cmd;

/**
 * 拼装 am instrument 命令。
 */
public final class TestCommand {

    private TestCommand() {
    }

    /**
     * 生成 am instrument 命令，在设备端脱离 PC 运行 uiautomator 用例。
     *
     * @param targetPkg uitest 模块 applicationId（instrumentation 目标 APK）
     * @param runner    runner 类全名
     * @param clsName   用例类全名
     * @param mtdName   用例方法名
     */
    public static String build(String targetPkg, String runner, String clsName, String mtdName) {
        return "am instrument -w -r -e debug false -e class "
                + clsName + "#" + mtdName
                + " " + targetPkg + "/" + runner;
    }
}