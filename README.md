### 依赖配置

---

添加 MethodTrace 依赖 [![](https://www.jitpack.io/v/vistring/AndroidMethodTrace.svg)](https://www.jitpack.io/#vistring/AndroidMethodTrace), 下面的 version 都用此版本代替

https://www.jitpack.io/#vistring/AndroidMethodTrace

> 添加 jitpack 仓库

```Grovvy
buildscript {
  repositories {
  	// ...
  	maven { url 'https://www.jitpack.io' }
  }
}
```

新的方式是

```Kotlin
pluginManagement {
    repositories {
        // ...
        maven { url = uri("https://www.jitpack.io") }
    }
}
```

> 添加 Gradle 插件的依赖

---

```kotlin
buildscript {
    // ...
    dependencies {
        classpath("com.vistring:method-trace:<version>")
    }
}
```

> 在 app 的 build.gradle.kts 中使用插件和配置基础信息

```Kotlin
plugins {
    // ...
    id("com.vistring.trace.method.plugin")
}

vsMethodTraceConfig {
  	// 比如下面两个包名前缀的都会被统计到
    includePackagePrefixSet = setOf(
        "com.vistring.trace.demo.view",
        "com.vistring.trace.demo.test",
    )
}
```

vsMethodTraceConfig 所有参数如下, 可自行配置：

```Kotlin
// 是否开启方法耗时统计
var enable: Boolean? = null
// 是否开启日志
var enableLog: Boolean? = null
// 是否开启高级匹配
var enableAdvancedMatch: Boolean? = null
// 包含的包名前缀
var includePackagePrefixSet: Set<String>? = null
// 排除的包名前缀
var excludePackagePrefixSet: Set<String>? = null
// 包含的包名正则
var includePackagePatternSet: Set<String>? = null
// 排除的包名正则
var excludePackagePatternSet: Set<String>? = null
```

> 添加 MethodTrace 的依赖库

```Kotlin
implementation("com.github.vistring.AndroidMethodTrace:method-trace:<version>")
```

> 输出效果

[点我请查看 log 文件](./traceLog.txt)