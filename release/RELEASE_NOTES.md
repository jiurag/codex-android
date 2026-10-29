# Codex 1.0.12（versionCode 21）

发布日期：2026-10-29

## 本版内容

1. 增加 GitHub Release 更新检查，应用启动时自动检查，也可在远程主机页手动检查。
2. 更新清单使用公开的 `latest.json`，包含版本号、APK 地址、文件大小和 SHA-256。
3. 下载完成后校验 SHA-256、包名、`versionCode` 和签名证书，防止安装错误或伪造 APK。
4. 更新包通过校验后交给 Android 系统安装器，由用户确认安装。
5. 远程主机页显示当前 App 版本。
6. APK 和 AAB 文件名统一改为 `codex-android-*`。

## 已验证

1. Debug 单元测试 86 项通过，失败 0、错误 0、跳过 0。
2. Release 单元测试 86 项通过，失败 0、错误 0、跳过 0。
3. API 35 模拟器设备黑盒测试 27 项通过，失败 0、错误 0、跳过 0。
4. 更新弹窗覆盖“发现更新”“下载完成”“校验失败”三种界面状态，按钮回调符合预期。
5. `lintDebug`、`lintVitalRelease`、R8、正式签名 APK 和 AAB 构建通过。
6. APK Signature Scheme v2 校验通过，正式包为 `com.codex.remote`、`versionCode 21`、`versionName 1.0.12`。

## 发布后验证

1. 公开仓库中的 `latest.json` 可匿名访问。
2. 清单中的 APK 地址、文件大小和 SHA-256 与实际资产一致。
3. `1.0.12` 能检查到后续版本，完成下载、签名校验并打开系统安装器。
