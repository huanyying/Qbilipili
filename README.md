# QbiliPlili

[![Build APK](https://github.com/huanyying/Qbilipili/actions/workflows/build.yml/badge.svg)](https://github.com/huanyying/Qbilipili/actions/workflows/build.yml)

一个 LSPosed / Xposed 模块：**劫持 QQ 内哔哩哔哩小程序卡片，一键跳转到 PiliPlus（第三方 B 站客户端）打开视频**，并自动跳过 QQ 的"即将离开 QQ"确认弹窗。

> ⚠️ 本项目仅供学习交流使用，请勿用于任何商业用途。Hook 第三方应用的行为可能违反其用户协议，风险自负。

## 特性

- 🎯 识别 QQ 内的哔哩哔哩小程序卡片（`miniAppId=1109937557`），提取其中的 `bvid`
- ⏭️ 用 `bilibili://video/{bvid}` 深链唤起 **PiliPlus** 打开视频，并阻断原小程序启动
- 🔕 自动点掉 QQ 的"离开 QQ"确认弹窗（`QQCustomDialog`），跳转无感无闪动
- 🎨 Material / MIUI 风格设置界面，支持 **跟随系统 / 浅色 / 深色** 三档主题
- 🔑 开关状态写入 QQ 自身 data 目录，需 root 授权后生效
- 📦 纯 Java 构建，无需 Gradle / aapt2，一条 `build.sh` 即可出包

## 环境要求

- Android 8.0+（hook 点基于较新 QQ 版本，兼容性以实测为准）
- ROOT（本模块实测基于 **KernelSU**，Magisk 理论上可用，需自行验证）
- [LSPosed](https://github.com/LSPosed/LSPosed)（或兼容 Xposed 框架）
- [PiliPlus](https://github.com/andforce/PiliPlus)（已注册 `bilibili://video` 深链）
- 官方 B 站 App（`tv.danmaku.bili`）可选，仅作备用

## 安装

1. 在 **LSPosed** 应用中启用 `QbiliPlili` 模块，勾选作用域 **QQ（com.tencent.mobileqq）**
2. 重启 QQ（或重新登录使其进程重启）
3. 打开 **QbiliPlili** 设置，在 KernelSU / 超级用户中授权后打开"启用跳转"开关
4. 在 QQ 内点开任意哔哩哔哩小程序卡片，应自动跳转 PiliPlus

> 若安装后模块未生效，请重启手机一次（LSPosed 需要刷新模块路径缓存）。
## 构建

```bash
cd qbiliplili
bash build.sh
# 产物：out/signed.apk，并复制到 /sdcard/QbiliPlili.apk
```

> 构建脚本为纯命令行流程（aapt 编译资源 → javac → r8 dex → 打包 → 签名），不依赖 Gradle。脚本顶部可通过修改 `JDK` / `TOOLS` 变量指定工具路径。

### 构建依赖获取

| 依赖 | 用途 | 获取方式 |
|---|---|---|
| JDK 17+ | javac / jarsigner / keytool | 任意发行版，如 [Temurin](https://adoptium.net/) |
| `android.jar`（platform 34） | 编译 Android API | Android SDK 的 `platforms/android-34/android.jar` |
| `aapt` | 编译资源/生成 arsc | Android SDK `build-tools/`，或 Debian 包 `aapt` |
| `r8.jar` | 打 dex | [R8 GitHub Releases](https://github.com/r8-releases/r8/releases) 下载 `.jar` |

默认工具路径假设为 `/opt/tools/`（含 `android.jar`、`r8.jar`、`aapt`），与构建机无关，可按需修改 `build.sh` 顶部变量。

### GitHub Actions 自动构建

仓库已配置 [GitHub Actions](.github/workflows/build.yml)：每次 push / PR 到 `main` 分支（或手动 `workflow_dispatch` 触发）都会自动执行完整构建，并把 `out/signed.apk` 作为构建产物（Artifact）供下载。CI 中通过环境变量 `JDK` / `TOOLS` 注入 JDK 与 android.jar/r8.jar 路径，无需修改 `build.sh`。


## 工作原理

- Hook `ContextWrapper` / `Activity` 的 `startActivity` / `startActivityForResult`，拦截 QQ 小程序启动 Intent
- 从 `KEY_APPINFO.launchParam.entryPath` 反射提取 `bvid`
- 命中后构造 `bilibili://video/{bvid}` 启动 PiliPlus，并 `setResult(null)` 阻断原跳转
- Hook `QQCustomDialog#show`：拦截"离开 QQ"弹窗，自动触发其确认按钮
- 开关状态存于 `/data/user/0/com.tencent.mobileqq/files/qbiliplili/enabled`，QQ 进程直接读取，Config 界面通过 `su` 读写

## 图标

桌面图标使用 [Pexels](https://www.pexels.com) 的二次元插画裁剪生成（源码 `gen/CropIcon.java` 可从 `gen/src_icon.jpg` 重新生成）。

## License

[MIT](LICENSE)
