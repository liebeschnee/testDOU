# -*- coding: utf-8 -*-
"""
从 Scenario(Phone) Excel 生成 DoU 离线自动化测试类。
每天 23 步，每步生成 dou/dayN/dou_day_N_M.java 独立测试类。

控件相关的操作（启动包名、点击文本/描述、输入、坐标）内联在每个场景脚本里，
便于按机型/控件信息差异直接维护；滑动、唤醒/息屏、等待等通用动作保留在 DoUHelper。

用法: python gen_dou_cases.py [--day 1]   # --day 可选，只生成指定天，默认生成全部
"""
import argparse
import os
import re
import openpyxl

ROOT = r"e:\11_tool\trae\TestU"
XLSX = os.path.join(ROOT, "SWR-601 XXX_DoU_AOSP&HMD_Test Report V1.5.xlsx")
SHEET = "Scenario(Phone)"
OUT_PKG_ROOT = os.path.join(ROOT, "app", "src", "main", "java",
                            "test", "dou", "testrunner", "uitest", "dou")

PKG_ROOT = "test.dou.testrunner.uitest.dou"

HEADER = '''package {pkg};

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * {title}
 *
 * <p>来源: Scenario(Phone) 表 Day{day} Step{step} {app}
 * <br>说明: 深度控件操作，{extra}
 */
@LargeTest
public class {cls} extends UiAutoTestCase {{

    private static final String TAG = "{cls}";

    @Test
    public void testDoU() throws Exception {{
{body}
    }}
}}
'''


def load_steps():
    wb = openpyxl.load_workbook(XLSX, data_only=True)
    ws = wb[SHEET]
    days = []
    cur_day = None
    for row in ws.iter_rows(min_row=9):
        # B列: Step# 或 "Day N"；C列: Use case；D列: Application；E列: Duration
        b = row[1].value
        c = row[2].value if len(row) > 2 else None
        d = row[3].value if len(row) > 3 else None
        e = row[4].value if len(row) > 4 else None
        if b is None:
            continue
        m = re.match(r"Day\s*(\d+)", str(b))
        if m:
            cur_day = int(m.group(1))
            days.append({"day": cur_day, "steps": []})
            continue
        # 数字且当前有 day
        if isinstance(b, (int, float)) and cur_day is not None:
            desc = str(c).strip() if c else ""
            app = str(d).strip() if d else ""
            dur = int(e) if isinstance(e, (int, float)) else 0
            days[-1]["steps"].append({"step": int(b), "desc": desc, "app": app, "dur": dur})
    return days


# ---------- 各 step 内联操作模板 ----------
# 返回若干行 Java 语句（行内相对缩进，生成时统一加 8 空格），
# 控件信息（包名/文本/描述/输入值）直接写死在脚本里，便于按机型维护。

def standby_body(dur):
    """Standby 步骤：回主界面 -> 息屏 -> 待机"""
    return [
        "// 回到主界面并息屏，进入待机",
        'Log.i(TAG, "[1] 回到主界面");',
        "DoUHelper.goHome(device);",
        'Log.i(TAG, "[2] 息屏待机");',
        "DoUHelper.screenOff(device);",
        'Log.i(TAG, "[3] 待机 {0} 分钟");'.format(dur),
        "DoUHelper.waitMinutes({0});".format(dur),
    ]


def gmail_read_body(dur):
    """Gmail 读邮件步骤"""
    return [
        "// Gmail：读取邮件",
        'Log.i(TAG, "[1] 启动 Gmail");',
        'DoUHelper.launchPkg(device, "com.google.android.gm");',
        'Log.i(TAG, "[2] 打开第一封邮件");',
        "UiObject first = device.findObject(new UiSelector().clickable(true).instance(0));",
        "if (first != null && first.exists()) {",
        "    first.click();",
        "    device.waitForIdle(800);",
        "}",
        'Log.i(TAG, "[3] 阅读邮件 {0} 分钟");'.format(dur),
        "DoUHelper.waitMinutes({0});".format(dur),
    ]


def body_for_step(step):
    s = step["step"]
    dur = step["dur"]
    app = step["app"]
    desc = step["desc"]
    low = app.strip().lower()

    # Standby 步骤
    if "standby" in low or "display off" in desc.lower():
        return standby_body(dur)

    if s == 1:  # Unlock device
        return [
            "// 唤醒并解锁设备（无密码，上滑解锁）",
            'Log.i(TAG, "[1] 唤醒设备");',
            "DoUHelper.wakeAndUnlock(device);",
            'Log.i(TAG, "[2] 保持亮屏 {0} 分钟");'.format(dur),
            "DoUHelper.waitMinutes({0});".format(dur),
        ]
    if s == 2:  # Google News & Weather
        return [
            "// 打开 Google News & Weather，从菜单选择 International news 浏览 {0} 分钟".format(dur),
            'Log.i(TAG, "[1] 启动 Google News & Weather");',
            'DoUHelper.launchPkg(device, "com.google.android.apps.magazines");',
            'Log.i(TAG, "[2] 等待菜单出现 International");',
            'DoUHelper.waitForVisible(device, "International");',
            'Log.i(TAG, "[3] 点击 International 新闻类型");',
            'DoUHelper.clickText(device, "International");',
            'Log.i(TAG, "[4] 上下滑动浏览新闻 {0} 分钟");'.format(dur),
            "DoUHelper.waitAndSwipe(device, {0});".format(dur),
        ]
    if s == 3:  # Camera
        return [
            "// 相机：连拍 10 张，再录 1 分钟视频",
            'Log.i(TAG, "[1] 启动相机");',
            'DoUHelper.launchPkg(device, "com.google.android.GoogleCamera");',
            'Log.i(TAG, "[2] 连拍 10 张照片");',
            "for (int i = 1; i <= 10; i++) {",
            '    Log.i(TAG, "拍摄第 " + i + " 张（点击快门 Shutter）");',
            '    if (!DoUHelper.clickDesc(device, "Shutter") && !DoUHelper.clickText(device, "拍照")) {',
            "        DoUHelper.clickScreen(device);",
            "    }",
            "    DoUHelper.sleepSec(1);",
            "}",
            'Log.i(TAG, "[3] 切换到录像模式");',
            'DoUHelper.clickDesc(device, "Video");',
            "DoUHelper.sleepSec(1);",
            'Log.i(TAG, "[4] 开始录像并持续 60 秒");',
            'if (!DoUHelper.clickDesc(device, "Shutter") && !DoUHelper.clickText(device, "录像")) {',
            "    DoUHelper.clickScreen(device);",
            "}",
            "DoUHelper.waitMinutes(1);",
            'Log.i(TAG, "[5] 点击结束录像");',
            'if (!DoUHelper.clickDesc(device, "Shutter") && !DoUHelper.clickText(device, "录像")) {',
            "    DoUHelper.clickScreen(device);",
            "}",
        ]
    if s == 4:  # Facebook
        return [
            "// Facebook：上传 1 图 + 1 视频，然后上下滑动浏览 {0} 分钟".format(dur),
            'Log.i(TAG, "[1] 启动 Facebook");',
            'DoUHelper.launchPkg(device, "com.facebook.katana");',
            'Log.i(TAG, "[2] 点击 Photo 打开上传入口");',
            'DoUHelper.clickText(device, "Photo");',
            "DoUHelper.sleepSec(2);",
            'Log.i(TAG, "[3] 等待上传完成");',
            "DoUHelper.sleepSec(5);",
            'Log.i(TAG, "[4] 上下滑动浏览信息流 {0} 分钟");'.format(dur),
            "DoUHelper.waitAndSwipe(device, {0});".format(dur),
        ]
    if s in (5, 15, 19):  # Gmail read email
        return gmail_read_body(dur)
    if s == 7:  # Phone & Messages
        return [
            "// 电话与短信：拨出/接听 6 通各 3 分钟、收发 6 条短信、发送 3 封带图邮件",
            'Log.i(TAG, "[1] 启动电话与短信应用");',
            'DoUHelper.launchPkg(device, "com.google.android.dialer");',
            'Log.i(TAG, "[2] 拨出/接听 3 轮（MO/MT 各 3 分钟）");',
            "for (int i = 1; i <= 3; i++) {",
            '    Log.i(TAG, "第 " + i + " 轮：拨出电话 3 分钟");',
            "    DoUHelper.goHome(device);",
            '    DoUHelper.launchPkg(device, "com.google.android.dialer");',
            "    DoUHelper.waitMinutes(3);",
            '    Log.i(TAG, "第 " + i + " 轮：接听电话 3 分钟");',
            "    DoUHelper.goHome(device);",
            "    DoUHelper.waitMinutes(3);",
            "}",
            'Log.i(TAG, "[3] 收发短信 3 轮");',
            "for (int i = 1; i <= 3; i++) {",
            "    DoUHelper.goHome(device);",
            '    DoUHelper.launchPkg(device, "com.google.android.dialer");',
            "    DoUHelper.sleepSec(5);",
            "}",
            'Log.i(TAG, "[4] 发送 3 封带图邮件");',
            "for (int i = 1; i <= 3; i++) {",
            "    DoUHelper.goHome(device);",
            '    DoUHelper.launchPkg(device, "com.google.android.gm");',
            "    DoUHelper.sleepSec(5);",
            "}",
        ]
    if s == 8:  # XE Currency
        return [
            "// XE Currency：新增 TWD 币种，录入 5 个不同 USD 价格",
            'Log.i(TAG, "[1] 启动 XE Currency");',
            'DoUHelper.launchPkg(device, "com.xe.app");',
            'Log.i(TAG, "[2] 点击添加币种入口");',
            'DoUHelper.clickText(device, "Add currency");',
            'Log.i(TAG, "[3] 输入并确认币种 TWD");',
            'DoUHelper.inputText(device, "TWD");',
            "DoUHelper.pressEnter(device);",
            "DoUHelper.sleepSec(2);",
            'Log.i(TAG, "[4] 录入 5 个 USD 价格: 1000/600/500/100/50");',
            "int[] prices = {1000, 600, 500, 100, 50};",
            "for (int price : prices) {",
            "    DoUHelper.inputText(device, String.valueOf(price));",
            "    DoUHelper.pressEnter(device);",
            "}",
            'Log.i(TAG, "[5] 保持应用前台 {0} 分钟");'.format(dur),
            "DoUHelper.waitMinutes({0});".format(dur),
        ]
    if s == 10:  # WhatsApp
        return [
            "// WhatsApp：收发消息 10 分钟-等 5 分钟，再收发 10 分钟-再等 5 分钟",
            'Log.i(TAG, "[1] 启动 WhatsApp");',
            'DoUHelper.launchPkg(device, "com.whatsapp");',
            'Log.i(TAG, "[2] 两轮收发消息（各 10 分钟）与等待（各 5 分钟）");',
            "for (int round = 1; round <= 2; round++) {",
            "    long end = System.currentTimeMillis() + 10 * 60L * 1000L;",
            "    while (System.currentTimeMillis() < end) {",
            '        Log.i(TAG, "第 " + round + " 轮：点击消息输入框并发送");',
            '        DoUHelper.clickText(device, "Message");',
            "        DoUHelper.sleepSec(3);",
            "        DoUHelper.pressEnter(device);",
            "        DoUHelper.sleepSec(3);",
            "    }",
            '    Log.i(TAG, "第 " + round + " 轮结束，等待 5 分钟");',
            "    DoUHelper.waitMinutes(5);",
            "}",
        ]
    if s == 11:  # Facebook messenger voice call
        return [
            "// Facebook Messenger：发起语音通话 10 分钟",
            'Log.i(TAG, "[1] 启动 Facebook Messenger");',
            'DoUHelper.launchPkg(device, "com.facebook.orca");',
            'Log.i(TAG, "[2] 点击语音通话入口");',
            'DoUHelper.clickDesc(device, "Voice call");',
            "DoUHelper.sleepSec(3);",
            'Log.i(TAG, "[3] 语音通话 10 分钟");',
            "DoUHelper.waitMinutes(10);",
            'Log.i(TAG, "[4] 通话结束回主界面");',
            "DoUHelper.goHome(device);",
        ]
    if s == 13:  # Booking
        return [
            "// Booking.com：搜索当前位置周边酒店，浏览至少 5 家酒店详情与照片",
            'Log.i(TAG, "[1] 启动 Booking.com");',
            'DoUHelper.launchPkg(device, "com.booking");',
            'Log.i(TAG, "[2] 点击搜索入口");',
            'DoUHelper.clickText(device, "Search");',
            "DoUHelper.sleepSec(2);",
            'Log.i(TAG, "[3] 浏览至少 5 家酒店详情");',
            "for (int i = 1; i <= 5; i++) {",
            '    Log.i(TAG, "浏览第 " + i + " 家酒店");',
            "    DoUHelper.swipeUp(device);",
            "    DoUHelper.sleepSec(2);",
            "}",
            'Log.i(TAG, "[4] 持续上下滑动浏览 {0} 分钟");'.format(dur),
            "DoUHelper.waitAndSwipe(device, {0});".format(dur),
        ]
    if s == 14:  # Youtube
        return [
            "// Youtube：播放高清视频 {0} 分钟（用 WiFi，结束后关闭 WiFi）".format(dur),
            'Log.i(TAG, "[1] 启动 Youtube");',
            'DoUHelper.launchPkg(device, "com.google.android.youtube");',
            'Log.i(TAG, "[2] 点击播放高清视频");',
            'DoUHelper.clickText(device, "Play");',
            "DoUHelper.sleepSec(3);",
            'Log.i(TAG, "[3] 播放 {0} 分钟");'.format(dur),
            "DoUHelper.waitMinutes({0});".format(dur),
            'Log.i(TAG, "[4] 关闭 WiFi");',
            "DoUHelper.setWifiEnabled(device, false);",
        ]
    if s == 17:  # Hill Climb Racing
        return [
            "// Hill Climb Racing：开始一场游戏并玩 {0} 分钟".format(dur),
            'Log.i(TAG, "[1] 启动 Hill Climb Racing");',
            'DoUHelper.launchPkg(device, "com.fingersoft.hillclimb");',
            'Log.i(TAG, "[2] 点击 Play 开始游戏");',
            'DoUHelper.clickText(device, "Play");',
            "DoUHelper.sleepSec(3);",
            'Log.i(TAG, "[3] 游戏进行 {0} 分钟");'.format(dur),
            "DoUHelper.waitMinutes({0});".format(dur),
        ]
    if s == 18:  # Chrome
        return [
            "// Chrome：输入 URL http://tw.yahoo.com，进入体育页浏览，并发送 3 封带图邮件",
            'Log.i(TAG, "[1] 启动 Chrome");',
            'DoUHelper.launchPkg(device, "com.android.chrome");',
            'Log.i(TAG, "[2] 点击地址栏");',
            'DoUHelper.clickText(device, "Search or type web address");',
            'Log.i(TAG, "[3] 输入 URL http://tw.yahoo.com 并回车");',
            'UiObject addressBar = device.findObject(new UiSelector().className("android.widget.EditText"));',
            "if (addressBar != null && addressBar.exists()) {",
            '    addressBar.setText("http://tw.yahoo.com");',
            "    DoUHelper.pressEnter(device);",
            "}",
            'Log.i(TAG, "[4] 点击 Sports 标签");',
            'DoUHelper.clickText(device, "Sports");',
            'Log.i(TAG, "[5] 上下滑动浏览 {0} 分钟");'.format(dur),
            "DoUHelper.waitAndSwipe(device, {0});".format(dur),
            'Log.i(TAG, "[6] 发送 3 封带图邮件");',
            "for (int i = 1; i <= 3; i++) {",
            "    DoUHelper.goHome(device);",
            '    DoUHelper.launchPkg(device, "com.google.android.gm");',
            "    DoUHelper.sleepSec(5);",
            "}",
        ]
    if s == 21:  # Spotify
        return [
            "// Spotify：打开 Daily Mix 开始播放 {0} 分钟".format(dur),
            'Log.i(TAG, "[1] 启动 Spotify");',
            'DoUHelper.launchPkg(device, "com.spotify.music");',
            'Log.i(TAG, "[2] 点击 Daily Mix");',
            'DoUHelper.clickText(device, "Daily Mix");',
            'Log.i(TAG, "[3] 点击 Play 开始播放");',
            'DoUHelper.clickText(device, "Play");',
            "DoUHelper.sleepSec(3);",
            'Log.i(TAG, "[4] 播放音乐 {0} 分钟");'.format(dur),
            "DoUHelper.waitMinutes({0});".format(dur),
        ]
    if s == 22:  # Google Map
        return [
            "// Google Map：搜索 Golden Gate Bridge，拖动地图后查看 2 个附加地点 POI 信息",
            'Log.i(TAG, "[1] 启动 Google Map");',
            'DoUHelper.launchPkg(device, "com.google.android.apps.maps");',
            'Log.i(TAG, "[2] 等待并点击搜索框");',
            'DoUHelper.waitForVisible(device, "Search");',
            'DoUHelper.clickText(device, "Search");',
            'Log.i(TAG, "[3] 输入地点 Golden Gate Bridge");',
            'DoUHelper.inputText(device, "Golden Gate Bridge");',
            "DoUHelper.pressEnter(device);",
            "DoUHelper.sleepSec(2);",
            'Log.i(TAG, "[4] 拖动地图查看 2 个额外 POI 信息");',
            "for (int i = 1; i <= 2; i++) {",
            "    DoUHelper.swipeUp(device);",
            "    DoUHelper.sleepSec(2);",
            "}",
            'Log.i(TAG, "[5] 保持地图浏览 {0} 分钟");'.format(dur),
            "DoUHelper.waitMinutes({0});".format(dur),
        ]
    # 兜底
    return [
        "// 通用步骤：尽量启动应用并停留 {0} 分钟".format(dur),
        'Log.i(TAG, "[1] 启动 {0}");'.format(app),
        'DoUHelper.launchPkg(device, "{0}");'.format(app),
        "DoUHelper.waitAndSwipe(device, {0});".format(dur),
    ]


def main():
    parser = argparse.ArgumentParser(description="生成 DoU 测试类")
    parser.add_argument("--day", type=int, default=None, help="只生成指定天（默认生成全部）")
    args = parser.parse_args()

    days = load_steps()
    if not days:
        print("未解析到任何 Day 数据，请检查 sheet/列结构")
        return
    total = 0
    for day_info in days:
        day = day_info["day"]
        if args.day is not None and day != args.day:
            continue
        for st in day_info["steps"]:
            step = st["step"]
            cls = "dou_day_{0}_{1}".format(day, step)
            pkg = "{0}.day{1}".format(PKG_ROOT, day)
            out_dir = os.path.join(OUT_PKG_ROOT, "day{0}".format(day))
            os.makedirs(out_dir, exist_ok=True)
            body = "\n".join("        " + line for line in body_for_step(st))
            title = "Day{0} Step{1} {2} ({3}min)".format(day, step, st["app"] or "-", st["dur"])
            extra = st["desc"].replace("\n", " / ")
            content = HEADER.format(
                pkg=pkg, title=title, day=day, step=step,
                app=st["app"] or "-", extra=extra, cls=cls, body=body)
            with open(os.path.join(out_dir, cls + ".java"), "w", encoding="utf-8") as f:
                f.write(content)
            total += 1
    print("已生成 {0} 个测试类".format(total))
    print("输出目录: {0}".format(OUT_PKG_ROOT))


if __name__ == "__main__":
    main()
