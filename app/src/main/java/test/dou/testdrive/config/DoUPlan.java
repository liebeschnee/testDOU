package test.dou.testdrive.config;

/**
 * DoU 场景计划元数据：每天步骤数、每个步骤对应的 uitest 用例类名。
 * 用例统一方法名为 testDoU，详见 uitest 模块 dou/dayN/dou_day_N_M.java。
 */
public final class DoUPlan {

    /** 总天数 */
    public static final int DAY_COUNT = 5;
    /** 每天步骤数 */
    public static final int STEP_COUNT = 23;

    /** uitest 用例所在包前缀 */
    private static final String CASE_PKG = "test.dou.testrunner.uitest.dou";

    private DoUPlan() {
    }

    /** 拼接某步的用例类全名，后续天复用 Day1 的用例类，如 dou_day_1_1 */
    public static String caseClass(int day, int step) {
        // 后续天复用 Day1 的用例类，避免重复文件
        return CASE_PKG + ".day1.dou_day_1_" + step;
    }

    /** 各步骤在二级菜单中的显示名（按 sheet Use case 概括） */
    public static String stepTitle(int step) {
        switch (step) {
            case 1: return "解锁设备";
            case 2: return "Google News 浏览新闻 20min";
            case 3: return "相机拍照 10 张+录像 1min";
            case 4: return "Facebook 上传+浏览 40min";
            case 5: return "Gmail 读邮件 2min";
            case 6: return "待机 Standby(83)";
            case 7: return "电话/短信 6 通+发邮件 30min";
            case 8: return "XE Currency 汇率换算 5min";
            case 9: return "待机 Standby(105)";
            case 10: return "WhatsApp 收发消息 30min";
            case 11: return "Messenger 语音通话 10min";
            case 12: return "待机 Standby(5)";
            case 13: return "Booking 查酒店浏览 20min";
            case 14: return "YouTube 高清视频 20min";
            case 15: return "Gmail 读邮件 5min";
            case 16: return "待机 Standby(125)";
            case 17: return "Hill Climb Racing 游戏 30min";
            case 18: return "Chrome 浏览+发邮件 30min";
            case 19: return "Gmail 读邮件 5min";
            case 20: return "待机 Standby(25)";
            case 21: return "Spotify 播放音乐 40min";
            case 22: return "Google Map 地图浏览 15min";
            case 23: return "待机 Standby(789)";
            default: return "Step " + step;
        }
    }
}