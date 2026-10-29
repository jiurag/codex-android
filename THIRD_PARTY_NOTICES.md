# 第三方资源说明

## Codex App 图标

Android 启动图标使用本机安装的 OpenAI Codex 桌面客户端应用清单实际引用的图标，原始文件为 `OpenAI.Codex 26.1016.8622.0` 的 `assets/Square44x44Logo.targetsize-256_altform-unplated.png`，尺寸为 256×256。Android 前景只增加透明留白并扩展到 1024×1024，不重绘官方图形；背景色 `#3143FF` 取自同一应用清单的 `BackgroundColor`。本项目不提供 `monochrome` 主题重绘图层，以免官方蓝底白标被系统替换成其他配色。

1. Codex App 官方介绍页：<https://developers.openai.com/codex/app>
2. 原始官方图标 SHA-256：`2B001FB1F4E7D76E4D459C2AD8F9739E963FFC1C2137ABCFFFE8DA3EE7775F7C`
3. Android 透明留白适配图 SHA-256：`CA907B9111E36D94768DB483C4C43DCC2A29ADA0528BD066A8A063F219F67078`

Codex 名称和图标相关权利归 OpenAI 所有。本项目是社区客户端，不代表 OpenAI 官方产品，也不表示获得 OpenAI 赞助或背书。

## MiSans

本 App 在界面中使用了小米 MiSans 字体。普通正文使用 `MiSans-Regular`，小号辅助信息使用 `MiSans-Medium`，标题与按钮使用 `MiSans-Semibold`。代码、路径、模型标识和差异内容继续使用 Android 等宽字体。

1. 官方项目页：<https://hyperos.mi.com/font/zh>
2. 官方下载页：<https://hyperos.mi.com/font/zh/download/>
3. 官方简体中文包：<https://hyperos.mi.com/font-download/MiSans.zip>
4. 官方许可协议：[MiSans-Font-License.pdf](licenses/MiSans-Font-License.pdf)
5. 官方简体中文包 SHA-256：`B6AA1FC827035922612DF8EDF36E5609BCA1C5441E25CD57572204569B7B81D9`
6. `MiSans-Regular.ttf` SHA-256：`9C120F0A849BC0AA5048DAAE2A3C0F6EECD828B5B33FCE682A9622833F5FEEA6`
7. `MiSans-Medium.ttf` SHA-256：`B03E98374E971594B0B7A9706D0704241F76E1B88556CDDA79C5039EF8A638D1`
8. `MiSans-Semibold.ttf` SHA-256：`77C23F31AE124867778344970155A0C8D34A89897DEDAAB81AEEE82FF00A4CE6`
9. 许可协议 PDF SHA-256：`4A93A27CD2BD81B3B5ECFD0A853144A876FA26938A93A68443C67D74172FCB86`

字体文件按官方原始文件内置，没有修改字体文件本身，也没有进行字形裁剪或二次开发。
