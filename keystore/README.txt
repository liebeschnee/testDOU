# 系统签名素材

本目录用于存放系统签名所需素材，使 APK 获得 `android.uid.system` 系统权限，
从而在设备端直接执行 `am instrument` 拉起 uiautomator 用例（离线运行关键前置）。

## 需要准备的素材（由系统源码/platform 侧提供）

| 文件 | 说明 |
| --- | --- |
| `platform.pk8`   | 系统 platform 私钥 |
| `platform.x509.pem` | 系统 platform 证书 |
| `platform.jks`   | 导入后的签名 KeyStore（本项目 build.gradle 引用） |

## 导入步骤

1. 从 AOSP 源码 `build/target/product/security/` 取得
   `platform.pk8` 与 `platform.x509.pem`（或由厂商提供）。

2. 用 `keytool-importkeypair` 把系统公私钥灌入项目签名 jks：

```
keytool -importkeystore \
  -noprompt \
  -srckeystore platform.pk8 \
  -srcstorepass android \
  -srcalias platform \
  -destkeystore platform.jks \
  -deststorepass android \
  -destalias platform

keytool-importkeypair -k platform.jks \
  -p android \
  -pk8 platform.pk8 \
  -cert platform.x509.pem \
  -alias platform
```

3. 在 `app/build.gradle` 中启用 `signingConfigs.platform` 并指向
   `keystore/platform.jks`（口令 `android`），用 release 变体打包。

4. 安装后验证系统权限已生效。

## 注意

- `android.uid.system` 的 APK 必须与目标平台同源的系统签名，否则无法安装/运行时被拒绝。
- 这些素材属于设备厂商保密信息，请勿提交到版本库。