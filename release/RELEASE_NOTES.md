# Codex 1.0.14（versionCode 23）

发布日期：2026-10-29

## 本版内容

1. 完成 `1.0.13 → 1.0.14` 的真实应用内下载与覆盖安装验证。
2. 保留 GitHub 官方域名白名单，不放行任意第三方跳转。
3. 保留 SHA-256、包名、`versionCode` 和签名证书校验。
4. 旧版本 `1.0.12` 因自身域名白名单缺陷无法自动升级，需手动安装一次 `1.0.13` 或更高版本。

## 自动构建验证

1. Debug 单元测试 86 项通过，失败 0、错误 0、跳过 0。
2. Release 单元测试 86 项通过，失败 0、错误 0、跳过 0。
3. API 35 模拟器设备黑盒测试 27 项通过，失败 0、错误 0、跳过 0。
4. 真实运行 `1.0.12` 时复现并定位 GitHub 官方资源域名跳转问题。
5. `lintDebug`、`lintVitalRelease`、R8、正式签名 APK 和 AAB 构建通过。
6. APK Signature Scheme v2 校验通过，正式包为 `com.codex.remote`、`versionCode 23`、`versionName 1.0.14`。

## 发布与真实升级验证

1. GitHub Actions 运行 `40875202245` 的 `verify` 和 `release` 均成功。
2. `v1.0.14` 为公开正式版本，标签指向提交 `98aaa187c516b5767d040ecd7b06fc517716ed32`。
3. 公开 `latest.json` 可匿名访问，内容为 `versionCode 23`、`versionName 1.0.14`。
4. 匿名下载的 APK 大小为 `20423573` 字节，SHA-256 为 `4f71c8f2735ee8665fea06db3892fed2d22020214e8d650bbd3bcc19433b9def`，与更新清单一致。
5. 在 API 35 模拟器中从已安装的 `1.0.13` 发起更新，界面成功显示 `1.0.13 → 1.0.14`，并完成下载与安全校验。
6. 通过 Android 系统安装器确认覆盖安装后，包信息为 `versionCode 23`、`versionName 1.0.14`。
7. 设备内安装后的 `base.apk` 大小和 SHA-256 与 GitHub Release 中的 APK 完全一致。
8. 更新完成后再次检查，界面显示“当前版本 1.0.14 已经是最新版本”。
9. Android 普通应用不能静默安装，首次更新需授权“安装未知应用”，并在系统安装器中确认。
