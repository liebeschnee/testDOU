# 续航模型测试工具（TestU / TestDrive）

Android 端 DoU（日常模拟使用）续航测试离线自动化工程。把 uiautomator2 测试用例与
宿主 UI 打包进**同一个系统签名 APK**，可在无 PC 的工程机上由 UI 一键触发整轮测试，
也可在 PC 上用 `adb am instrument` 单条或批量调试。

- 应用名：续航模型测试工具
- 包名 / applicationId：`test.dou.testdrive`
- 测试用例包：`test.dou.testrunner.uitest`
- 说明：测试用例与 runner 均打进 Host APK，`am instrument` 以自身为 targetPackage。

---

## 1. 特点

- **系统签名**（`android.uid.system`）：工程机可绕过权限限制，直接在 App 内
  `Runtime.exec("sh")` 执行 `am instrument` 拉起 uiautomator 用例，实现 UI 一键跑整轮测试。
- **单 APK**：无需另装 uiautomator runner，Host + 用例合一个 `app-release.apk`。
- **结构化脚本**：23 个场景，每个场景一个独立测试类 `dou/day1/dou_day_1_N.java`，
  控件信息（包名/文本/描述/输入值）内联在脚本里，按机型差异可直接修改；
  后续动作循环**复用同一套 Day1 用例，不做任何回填/修改**。
- **不限天数的循环测试**：主页“开始循环测试”从第 1 轮起，串行执行 23 个场景，
  跑完一轮轮次号自增，**一直循环直到设备因电量耗尽自动关机**（或用户点“停止”）。
- **电量报告落盘**：每个场景 `@Before`/`@After` 各记录一次电量
  （百分比 % 与剩余容量 mAh），每条数据在步骤结束后**立即写文件并 fsync**，
  确保设备在电量耗尽瞬间（突然关机/断定）前已落盘的数据不丢失，
  输出目录 `/sdcard/DOUreport/`，每天（轮）一个 CSV。
- **进度持久化**：每轮当前 23 步状态（PENDING/RUNNING/PASS/FAIL/SKIPPED）
  用 SharedPreferences 保存，退出重进 App 仍可见；运行中“开始测试”按钮置灰不可点。

---

## 2. 目录结构

```
TestU/
├─ app/
│  ├─ src/main/java/
│  │  ├─ test/dou/testdrive/          # 宿主 App
│  │  │  ├─ MainActivity.java         # 主页：当前轮次 23 步状态 + 整体进度条
│  │  │  ├─ ui/
│  │  │  │  ├─ DayActivity.java       # 二级菜单：某轮 23 步状态 + 前后电量，可单跑一步
│  │  │  │  └─ TestLauncher.java      # 工作者线程串行执行 am instrument、循环调度、回收结果
│  │  │  ├─ cmd/
│  │  │  │  ├─ TestCommand.java       # 拼装 am instrument 命令
│  │  │  │  └─ CMDUtils.java          # shell 执行（sh/su）与可中断的命令会话
│  │  │  ├─ config/
│  │  │  │  ├─ DoUPlan.java           # 步数/用例类名映射/步骤标题
│  │  │  │  └─ TestConfig.java        # targetPackage、runner、日志与报告落盘目录
│  │  │  └─ report/
│  │  │     ├─ ProgressStore.java     # 每步进度/电量持久化（SharedPreferences）
│  │  │     ├─ DouReport.java         # 每轮每场景电量报告（/sdcard/DOUreport/day_<N>.csv）
│  │  │     └─ LogCollector.java      # am instrument 输出落盘
│  │  └─ test/dou/testrunner/uitest/  # 测试用例
│  │     ├─ UiAutoTestCase.java       # 基类：初始化 UiDevice、前后电量记录
│  │     ├─ PopupMonitor.java         # 后台弹框监控，自动关闭 ANR/权限等弹框
│  │     ├─ util/
│  │     │  ├─ DoUHelper.java         # 通用手势/系统动作 + 控件原语（含 readBattery）
│  │     │  └─ UiDump.java            # UI 树 dump / 截图 / 文本断言
│  │     └─ dou/day1/dou_day_1_1..23.java  # 23 条场景用例（循环测试复用同一套，不新增）
│  ├─ src/main/AndroidManifest.xml
│  └─ build.gradle
├─ gen_dou_cases.py                    # 由 Excel 生成测试类（--day 只生成指定天）
├─ platform.pk8 / platform.x509.pem    # AOSP 官方默认系统测试密钥
├─ signapk.jar / apksigner.jar         # 备用签名工具
├─ keystore/
│  └─ platform-aosp.jks               # 由 pk8+pem 导入的签名 KeyStore（口令 android）
└─ SWR-601 ..._Test Report V1.5.xlsx  # 用例来源表（Scenario(Phone) sheet）
```

---

## 3. 编译打包

前置：JDK17 + Android Gradle Plugin 8.1.4，`local.properties` 配好 SDK 路径。

```
:: 生成 release APK（走系统签名 signingConfigs.platform）
gradlew.bat :app:assembleRelease
```

产物：`app\build\outputs\apk\release\app-release.apk`

签名说明（见 `keystore/README.txt`）：
- 使用 AOSP 官方默认 platform 测试密钥（`platform.pk8` + `platform.x509.pem`）。
- 已灌入 `keystore/platform-aosp.jks`（storePassword/keyAlias/keyPassword 均为 `android`），由
  [app/build.gradle](file:///e:/11_tool/trae/TestU/app/build.gradle) 的 `signingConfigs.platform` 引用。
- `platform-aosp.jks` 需与目标工程机同源系统签名，否则报 `INSTALL_FAILED_SHARED_USER_INCOMPATIBLE`。

---

## 4. 安装与运行

### 4.1 安装

```
adb install -r app\build\outputs\apk\release\app-release.apk
```

### 4.2 App 内一键跑（离线，无 PC，主线流程）

1. 桌面打开“续航模型测试工具”。
2. 点“开始循环测试”：从第 1 轮开始，串行执行 23 个场景（复用 Day1 用例类），
   每一步状态的开始/结束电量（% + mAh）立即写入 `/sdcard/DOUreport/day_<N>.csv`。
3. 跑完一轮轮次号自增，回到第 1 步继续下一轮，**不限天数，一直循环直到电量耗尽自动关机**；
   运行中按钮置灰“测试运行中...”。
4. 点步骤行进入二级菜单，可查看某轮每步状态及“测试前/测试后”电量，也可单独运行某一步。
5. 需人工结束时点“停止”：发出停止信号、结束当前步骤、把中断的步骤标 FAIL/SKIPPED。

> 前置：工程机已预装被测应用（Google News、相机、Facebook、Gmail、WhatsApp 等）；
> 未预装的包在脚本里 `launchPkg` 会回 Home、不阻断。

---

## 5. 电量报告（/sdcard/DOUreport/）

循环测试每跑完一轮生成一个 CSV：`/sdcard/DOUreport/day_<轮次>.csv`，
记录该轮**每个场景**的开始/结束电量（百分比与剩余容量 mAh）：

```
# day_1.csv 示例
Step,Title,Before%,Before(mAh),After%,After(mAh),Status,Timestamp
1,解锁设备,91,4525,90,4500,PASS,2026-09-11 09:00:00
2,Google News 浏览新闻 20min,90,4500,86,4280,PASS,2026-09-11 09:21:00
...
23,待机 Standby(789),57,2840,55,2750,PASS,2026-09-11 23:05:00
```

- `Before%/Before(mAh)`：该场景 @Before 记录；`After%/After(mAh)`：@After 记录。
- 每条数据写入后立即 `flush` + `fd.sync()`，设备在电量耗尽瞬间突然关机/断定时，
  已完成的场景数据已真正落盘、不会丢失。
- 报告目录见 `TestConfig.DOU_REPORT_DIR`（缺省 `/sdcard/DOUreport`），创建失败会回退应用私有目录。
- 电量来源：`DoUHelper.readBattery()`（BatteryManager 的 CAPACITY + CHARGE_COUNTER，回退 sysfs）。

---

## 6. PC 调试命令

### 6.1 单条用例（常用）

```
adb shell am instrument -w -r -e debug false -e class test.dou.testrunner.uitest.dou.day1.dou_day_1_2#testDoU test.dou.testdrive/androidx.test.runner.AndroidJUnitRunner
```

- 换用例只改 `#` 前的类名，如 `dou_day_1_1` ... `dou_day_1_23`。
- 方法统一为 `testDoU`。
- 脚本拼装逻辑见 [TestCommand.java](file:///e:/11_tool/trae/TestU/app/src/main/java/test/dou/testdrive/cmd/TestCommand.java)。

### 6.2 全部用例（23 条，复用 Day1，慎用，耗时长）

```
adb shell am instrument -w -r -e debug false test.dou.testdrive/androidx.test.runner.AndroidJUnitRunner
```

> 注意：在 **CMD** 中请保持命令单行；不要用 PowerShell 反引号 `` ` `` 做换行，
> 否则会被当作命令替换截断 `-e class` 参数，导致跑成全部用例而非指定的单条。

### 6.3 查看电量 / 日志 / 报告

```
adb logcat -s TestLauncher System.out          # 每步调度 + DOU_BATTERY_BEFORE/AFTER 电量标记
adb pull /sdcard/DOUreport/                    # 每天（轮）每场景电量报告 CSV
adb pull /sdcard/TestDrive/log/result.txt      # am instrument 结果落盘
```

---

## 7. 结果与落盘位置

| 内容 | 路径 |
| --- | --- |
| 每轮每场景电量报告（% + mAh） | `/sdcard/DOUreport/day_<轮次>.csv` |
| 电量标记（logcat） | `DOU_BATTERY_BEFORE:` / `DOU_BATTERY_AFTER:` |
| UI 树 dump | `/sdcard/TestDrive/run/ui_<时间戳>.xml` |
| 关键步骤截图 | `/sdcard/TestDrive/run/step_<name>_<时间戳>.png` |
| am instrument 输出 | `/sdcard/TestDrive/log/result.txt` |
| 当前轮次每步状态（App 内） | SharedPreferences `dou_progress` |

---

## 8. 用例与生成

- 每条用例继承 [UiAutoTestCase.java](file:///e:/11_tool/trae/TestU/app/src/main/java/test/dou/testrunner/uitest/UiAutoTestCase.java)，
  含唯一 `@Test testDoU()`，自动记录前后电量（`DOU_BATTERY_BEFORE/AFTER`）。
- 通用动作在 [DoUHelper.java](file:///e:/11_tool/trae/TestU/app/src/main/java/test/dou/testrunner/uitest/util/DoUHelper.java)；
  控件操作内联在各 `dou_day_1_N.java`（启动包名/文本/描述/输入值直接可见可改）。
- 由 Excel（`Scenario(Phone)` sheet）生成：

```
:: 重新生成全部天
python gen_dou_cases.py
:: 只重新生成第 1 天
python gen_dou_cases.py --day 1
```

> 循环测试复用第 1 天的 23 条用例（`DoUPlan.caseClass` 恒指向 `day1.dou_day_1_N`），
> 无需为第 2 天以后生成或维护用例文件。

---

## 9. 常见问题

- **`INSTALL_FAILED_SHARED_USER_INCOMPATIBLE`**：签名与设备系统签名不一致，换用 AOSP platform 密钥重签。
- **跑成全部用例**：多出现在 CMD 用了反引号换行导致 `-e class` 被截断，改用单行命令。
- **无外部存储权限**：系统签名下通常可直写 `/sdcard`；普通签名走应用私有目录回退。
- **App 无法执行 am instrument**：非系统签名时 `Runtime.exec("sh")` 会被权限拒绝，需系统签名。
- **某一步因低电量被关机打断**：该步未写入 AFTER 属正常；已完成的场景均已 fsync 落盘，重启后可继续点“开始”重跑。