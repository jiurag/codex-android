# Codex Remote Android

通过 SSH 连接远端 Codex 主机的 Android 客户端。Codex 运行在服务器上；App 负责读取项目与会话，并通过 `codex app-server` 的 JSONL 协议执行任务。

本项目基于 [liuhao-labs/codex-remote-android](https://github.com/liuhao-labs/codex-remote-android) 修改和维护，不是 OpenAI 官方产品。

## 主要功能

- 完整简体中文界面，内置小米 MiSans 字体。
- 支持 DeepSeek 等由远端 `model/list` 返回的模型。
- 支持连接已有会话、新建任务、运行中追加输入和审批操作。
- 支持 CC Switch 供应商余额与 Codex 上下文用量显示。
- 适配状态栏、手势区、横屏、键盘安全区和较大字体。
- 针对 `codex-cli 0.165.0` 适配协作模式、新会话和 `functionCallOutput`。

## 使用条件

- 服务器可通过 OpenSSH 连接，并支持密码或私钥认证。
- 远端登录 shell 可直接执行 `codex --version`。
- 远端 Codex 已完成模型和账号配置。
- 建议使用权限较小的专用 SSH 用户。

## 安装与使用

1. 从仓库的 **Releases** 页面下载 APK。
2. 安装后添加服务器地址、端口、用户名和凭据。
3. 核对首次连接显示的主机指纹。
4. 连接服务器，选择项目或新建会话。
5. 在输入栏选择模型和权限档位后发送任务。

如果手机上安装过不同签名的旧包，需要先卸载旧包再安装正式版；卸载会清除 App 本地保存的连接信息。

## 构建

需要 JDK 17 和 Android SDK 35：

```powershell
.\gradlew.bat test lintDebug assembleDebug
```

构建签名 Release：

```text
CODEX_REMOTE_KEYSTORE_PATH
CODEX_REMOTE_KEYSTORE_PASSWORD
CODEX_REMOTE_KEY_ALIAS
CODEX_REMOTE_KEY_PASSWORD
```

配置以上环境变量后执行：

```powershell
.\gradlew.bat assembleRelease bundleRelease
```

签名文件、构建产物和本机配置不得提交到仓库。

## 自动发布

推送 `main` 后，GitHub Actions 会依次执行测试、Lint、Debug 构建、签名 Release 构建和验签。成功后创建或更新 `v<versionName>` Release，并上传 APK、AAB 与 `SHA256SUMS.txt`。

签名材料通过以下 Actions Secrets 注入：

- `CODEX_REMOTE_KEYSTORE_BASE64`
- `CODEX_REMOTE_KEYSTORE_PASSWORD`
- `CODEX_REMOTE_KEY_ALIAS`
- `CODEX_REMOTE_KEY_PASSWORD`

## 已知限制

- 当前只提供简体中文，没有语言切换。
- 主要在 Android API 35 模拟器完成测试，尚未覆盖多款真实手机。
- GitHub Release 不是 Google Play 上架。
- 上游尚未实现的功能仍可能缺失。

更多协议与功能记录见 [Codex 0.165 黑盒记录](docs/BLACKBOX_CODEX_0165.md) 和 [功能对照](docs/FEATURE_PARITY.md)。

## 授权

本项目沿用上游的 [Apache License 2.0](LICENSE)。
