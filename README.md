# VidX Player - 高性能 Android 掌上媒体播放器

VidX Player 是一款专为高端掌上娱乐与多媒体演示设计的高性能 Android 本地/网络流媒体播放器。应用采用现代 Android 架构设计、响应式 Jetpack Compose 界面与谷歌官方 Media3 ExoPlayer 内核，实现了本地视频智能索引、手势控制、极简网络直播源、本地 DLNA TV 投屏、Room 历史记录断点播放、画中画（PiP）播放，并全面配合了 Material 3「**专业典雅 (Professional Polish)**」的主题配色与**自适应科技矢量图标 (Adaptive Icon)**。

项目已完全汉化（包括操作界面、权限提醒、手势 HUD、倍速调节、投屏发现列表与弹窗交互等）。

---

## 🚀 核心功能特性

1. **自动媒体库与文件夹检索**：
   - 支持动态权限检查及一键快速重新扫描，遍历手机物理存储设备提取所有格式的本地视频。
   - 按物理文件夹分类聚类生成视频索引卡片，直观展现文件总数、总大小与播放分辨率。

2. **高级播放手势与 HUD 系统**：
   - **屏幕左侧滑动**：无级精确调节系统屏幕亮度（带磨砂黑色极简 HUD 视觉进度反馈）。
   - **屏幕右侧滑动**：无级精确调节音量大小（结合设备扬声器物理增幅，带音量进度 HUD 图形指示器）。
   - **两侧双击交互**：左侧双击快退 10 秒，右侧双击快进 10 秒，带有醒目的悬浮动画气泡提示。
   - **手势及布局锁定**：通过控制条底部的实体锁按钮一键屏蔽屏幕全局触摸干扰。

3. **智能 M3U8 HLS 极速直播源**：
   - 允许用户动态输入名称和 URL，注入任何合法的 HLS IPTV 直播流源（m3u8 格式），并提供简易的删除和收藏管理。

4. **万能 DLNA 智能电视多路投屏**：
   - 基于本地 SSDP 组播发现机制，点击即可在后台扫描同一局域网（Wi-Fi）下的所有智能电视和投屏渲染设备，并支持一键投射、遥控与播放同步。

5. **智能播放断点历史与 Room 持久化**：
   - 每次播放视频时自动在本地 SQLite/Room 数据库记录播放进度。下一次启动时完美展现已 watched 进度比例，并支持直接断点续播，同时支持历史清除。

6. **画中画（PiP）随心看**：
   - 用户在播放任何视频时按下 Home 键返回桌面，系统会自动激活画中画视窗浮窗，不干扰多任务并行处理。

---

## 🎨 专业典雅 (Professional Polish) 核心设计主题

VidX Player 精确还原了设计美学的精髓，整体视觉极具现代感与通透度：
* **色彩系统（Professional Blue Palette）**：
  - **主色调**：金字塔深海皇家蓝（`#0061A4` ），尽显深邃商务尊贵、稳定可靠。
  - **辅助强调色**：冰晶蓝（`#D1E4FF` ），作为按键浮窗及高亮状态容器，视觉呼吸感拉满。
  - **背景与表面（Surface & Canvas）**：高阶深灰（`#101214` ）、暗晶圆润卡片（`#1A1C1E`），搭配超低不透明度白色层，提供出众的层级微晶玻璃流光质感。
* **圆角与边框**：全系按钮及弹窗卡片使用 `12.dp` 至 `16.dp` 大圆角高阶拟物处理，辅以細微半透明白色（或灰色）描边，消除视觉冗余与低端感。

---

## 🎯 自适应科技图标 (Adaptive Launcher Icon)

根据 Material You 图标设计指南，为 VidX Player 设计并替换了全新的矢量自适应图标：
* **背景层 (`ic_launcher_background.xml`)**：自 top-left 朝 bottom-right 的斜角深蓝色渐变（`#0061A4` 到 `#001D36` ），伴随极简网格透视几何亮线，烘托数字底座。
* **前景层 (`ic_launcher_foreground.xml`)**：中心完美容纳在 **66dp** 安全范围。由一个带有磨砂半透明白灰交叠的艺术圆环，嵌套亮蓝主色调，内置极简切割白金播放三角标志（Play Logo），兼顾单色变体 (Monochrome) 适配。

---

## 📁 目录结构

```text
/app/src/main/
├── AndroidManifest.xml             # 系统权限配置、PiP、多任务过滤与 Activity 声明
├── java/com/example/
│   ├── MainActivity.kt             # 主要承载容器、动态运行时权限拦截、系统画中画（PiP）切换
│   ├── data/
│   │   ├── database/               # Room 核心数据库：PlayHistory（观看纪录实体层）、LiveStream（网络直播源实体层）、AppDatabase 及 DAO
│   │   ├── dlna/                   # SSDP/UDP 组播发现本地 DLNA/UPnP TV 接收设备引擎
│   │   └── repository/             # 本地 MediaStore 查询库，专责视频文件的递归索引提取、分辨率转换与格式化
│   └── ui/
│       ├── screens/
│       │   ├── MainDashboard.kt     # 四大核心 Tab 顶级大看板（扫描控制区、本地文件夹网格、M3U8 直播管理区、以及电视遥控历史栏）
│       │   └── VideoPlayerScreen.kt # 超燃手势、音量亮度 HUD、外挂字幕选择器、3.0x倍速面板等。
│       ├── theme/
│       │   ├── Color.kt            # 淬炼自 Professional Polish HTML 设计的可视颜色系统定义
│       │   ├── Theme.kt            # MyApplicationTheme 控制中心（暗色/亮色自适配）
│       │   └── Type.kt             # Material 3 高端无衬线级排版字体定义
│       └── viewmodel/
│           └── MediaViewModel.kt   # 控制媒体加载、异步搜索过滤、TV 信号推送、以及 Room 本地交互的完整 VM 层
└── res/
    ├── drawable/
    │   ├── ic_launcher_background.xml # 酷炫微晶科技网络背景
    │   └── ic_launcher_foreground.xml # 66dp安全区高对比亮白金播放器图标
    └── values/
        └── strings.xml             # 软件应用属性声明
```

---

## 🛠 开发与构建指南

### 1. 先决条件
- **JDK**：版本 17 或以上
- **Gradle**：支持 Kotlin DSL （`.gradle.kts`）构建配置
- **Android SDK 支持**：
  - `compileSdk` / `targetSdk`: 34 (Android 14)
  - `minSdk`: 26 (Android 8.0, 确保对 ExoPlayer 及系统 PiP 的极佳运行支持)

### 2. 跑单元测试与 Roborazzi 截图验证
- **本地单元与 Robolectric 测试**：
  ```bash
  gradle :app:testDebugUnitTest
  ```
- **记录参考屏幕截图 (Roborazzi)**：
  ```bash
  gradle :app:recordRoborazziDebug
  ```
- **进行自动化视觉回归跑测**：
  ```bash
  gradle :app:verifyRoborazziDebug
  ```

---

## 💡 运行时用户体验小技巧

1. **第一次加载**：未授予存储权限时，应用将展示由 Professional 皇家蓝点缀的高对比度「**需要存储空间访问权限**」提示页。当您授权后，VidX 瞬间为您构建完整的本地视频媒体矩阵。
2. **手势调速**：播放本地或 M3U8 流时，点触右上角的「**速度**」按钮，能弹出一款符合全面半透明大圆格的调速框，点击从减慢 4 倍到暴增 4 倍等 10 种播放模式。
3. **M3U8 播放**：若无本地文件，可在“直播源”选项卡中点击右上角 `+` 号输入一些网络公开的 M3U8 电视频道或演示源（例如一些标准央视或卫视组播源），一触秒播，流畅不卡顿。
