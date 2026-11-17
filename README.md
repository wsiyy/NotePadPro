# 记事本 NotePad Pro

移动软件开发课程**期中实验**：基于 NotePad 记事本应用的功能扩展。

在经典 NotePad（笔记列表 / 编辑 / SQLite 存储）的基础上，完成了课程要求的全部基本功能，
并额外实现了 4 项扩展功能。

---

## 一、功能清单

### 基本要求（必做）

| 序号 | 要求 | 实现说明 |
| --- | --- | --- |
| 1 | NoteList 界面笔记条目增加**时间戳**显示 | 每条笔记底部显示更新时间；今天显示「今天 HH:mm」、昨天显示「昨天 HH:mm」、今年显示「MM-dd HH:mm」、更早显示完整日期 |
| 2 | 添加笔记**查询**功能（按标题或内容） | 标题栏 SearchView，输入即时过滤；SQL 使用 `title LIKE ? OR content LIKE ?`，标题与正文同时匹配 |

### 附加功能（至少两项，本项实现 4 项）

| 序号 | 功能 | 说明 |
| --- | --- | --- |
| 1 | **笔记分类** | 新建/编辑时选择分类（工作/学习/生活/灵感/其他），列表顶部 Chip 一键筛选 |
| 2 | **深色模式** | 基于 DayNight 主题，菜单切换后立即生效并持久化（SharedPreferences） |
| 3 | **笔记置顶** | 长按笔记可置顶/取消置顶，置顶笔记始终排在最前 |
| 4 | **分享与导出** | 编辑器内可分享为纯文本，或通过系统文件选择器（SAF）导出为 `.txt` 文件 |

### UI / 交互细节

- 卡片式列表（MaterialCardView），标题、两行摘要、分类标签、时间戳四层信息
- 空列表引导文案
- 返回键自动保存，空笔记不落库
- 编辑页实时显示「创建于 … · 更新于 …」

---

## 二、运行环境

| 项目 | 版本 |
| --- | --- |
| Android Studio | Ladybug / Meerkat 及以上 |
| JDK | 17 |
| Gradle | 8.7（`gradle/wrapper/gradle-wrapper.properties` 已配置） |
| AGP | 8.5.2 |
| compileSdk / targetSdk | 34 |
| minSdk | 26（Android 8.0） |

### 打开方式

**方式一（推荐）**：Android Studio → `File > New > Import Project`，选择本目录。
若提示缺少 `gradle-wrapper.jar`，任选其一：

1. 在 AS 的 Terminal 中执行 `gradle wrapper --gradle-version 8.7`（需本机已安装 Gradle）；
2. `Settings > Build, Execution, Deployment > Build Tools > Gradle` 中把
   `Use Gradle from` 改为本地 Gradle 发行版；
3. 新建一个空项目，把本项目的 `app/src/main`、`app/build.gradle`、`settings.gradle`、
   `build.gradle` 拷贝过去（包名保持 `com.example.notepadpro`）。

**方式二**：命令行

```bash
gradle wrapper --gradle-version 8.7   # 首次生成 wrapper（可选）
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

---

## 三、项目结构

```
NotePadPro/
├── build.gradle                      # 顶层构建脚本（AGP 8.5.2）
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/notepadpro/
        │   ├── NotePadApp.java       # Application，恢复深色模式
        │   ├── NoteListActivity.java # 列表页：时间戳 / 搜索 / 分类 / 置顶
        │   ├── NoteEditActivity.java # 编辑页：保存 / 分享 / 导出
        │   ├── NoteAdapter.java      # RecyclerView 适配器
        │   ├── Note.java             # 实体
        │   ├── NoteDbHelper.java     # SQLite 增删改查
        │   ├── Prefs.java            # 偏好设置
        │   └── TimeUtils.java        # 时间戳格式化
        └── res/
            ├── layout/               # 列表页、编辑页、列表项
            ├── menu/                 # 列表菜单、编辑菜单
            ├── drawable/             # 图标、分类标签背景、启动图标
            ├── values/               # 字符串、颜色、主题
            ├── values-night/         # 深色模式配色
            └── mipmap-anydpi-v26/    # 自适应图标
```

---

## 四、数据库设计

表 `notes`（`notepad.db`，版本 1）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `_id` | INTEGER PK AUTOINCREMENT | 主键 |
| `title` | TEXT | 标题，可为空 |
| `content` | TEXT | 正文 |
| `created_at` | INTEGER | 创建时间（毫秒） |
| `updated_at` | INTEGER | 更新时间（毫秒） |
| `category` | TEXT | 分类，默认「其他」 |
| `pinned` | INTEGER | 是否置顶，0/1 |

列表查询语句（搜索 + 分类 + 排序）：

```sql
SELECT * FROM notes
WHERE 1 = 1
  AND category = ?                       -- 分类为「全部」时不拼接
  AND (title LIKE ? OR content LIKE ?)   -- 关键字为空时不拼接
ORDER BY pinned DESC, updated_at DESC;
```

---

## 五、关键实现

### 1. 时间戳显示

`TimeUtils.format(long millis)` 按「今天 / 昨天 / 今年 / 更早」四级格式化，
避免列表里出现一长串完整日期，同时保证信息完整：

```java
if (isSameDay(today, target))     return "今天 " + hm.format(date);
if (isSameDay(yesterday, target)) return "昨天 " + hm.format(date);
if (today.get(Calendar.YEAR) == target.get(Calendar.YEAR)) {
    return new SimpleDateFormat("MM-dd HH:mm", ...).format(date);
}
return new SimpleDateFormat("yyyy-MM-dd HH:mm", ...).format(date);
```

### 2. 搜索

`SearchView.OnQueryTextListener` 的 `onQueryTextChange` 中直接刷新列表，
查询条件拼进 SQL，`LIKE` 通配符为 `%关键字%`，同时命中标题与正文。

### 3. 分类筛选

`ChipGroup` 代码动态生成 6 个 Chip，`singleSelection + selectionRequired`
保证始终有一项被选中；点击后把分类名作为 SQL 条件重新查询。

### 4. 深色模式

`Theme.MaterialComponents.DayNight.NoActionBar` + `values-night/colors.xml`；
切换时 `AppCompatDelegate.setDefaultNightMode(...)`，Activity 自动重建，
选择结果写入 `SharedPreferences`，`Application.onCreate` 中恢复。

### 5. 导出

`ActivityResultContracts.CreateDocument("text/plain")` 调起系统文件选择器，
拿到 `Uri` 后通过 `ContentResolver.openOutputStream()` 写入 UTF-8 文本，无需存储权限。

---

## 六、上传到 GitHub

```bash
cd NotePadPro
git init
git add .
git commit -m "期中项目：NotePad 记事本应用扩展（时间戳/搜索/分类/深色模式/导出）"
git branch -M main
git remote add origin https://github.com/<你的GitHub用户名>/NotePadPro.git
git push -u origin main
```

> 提交前建议在真机/模拟器上运行一遍并截图，把图片放到 `docs/screenshots/`，
> 再在 README 中引用，项目主页会更直观。

## 七、CSDN 博客

博客草稿见 `docs/CSDN博客草稿.md`，按其中的占位符替换截图与个人信息后，
发布到班级指定的 CSDN 账号即可。
