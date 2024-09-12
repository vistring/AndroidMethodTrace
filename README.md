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

// 配置插件. 建议使用白名单机制, 例如下面的
vsMethodTraceConfig {
    // 配置耗时阈值
    costTimeThreshold = 80L
    // 比如下面两个包名前缀的都会被统计到
    /*includePackagePrefixSet = setOf(
        "com.vistring.trace.demo.view",
        "com.vistring.trace.demo.test",
    )*/
    // 比如排除掉所有被下面注解标记的方法
    /*excludeMethodAnnoSet = setOf(
        "androidx.annotation.MainThread",
    )*/
}
```

vsMethodTraceConfig 所有参数如下, 可自行配置：

```Kotlin
// 匹配全部, 默认为 false 如果开启此配置. 
// 请一定要阅读文档最后建议排除的一些包名和一些方法
var matchAll: Boolean? = null,
// 方法耗时阈值
var costTimeThreshold: Long? = null
// 是否开启方法耗时统计
var enable: Boolean? = null
// 是否开启日志
var enableLog: Boolean? = null
// 包含的包名前缀
var includePackagePrefixSet: Set<String>? = null
// 排除的包名前缀
var excludePackagePrefixSet: Set<String>? = null
// 包含的包名正则
var includePackagePatternSet: Set<String>? = null
// 排除的包名正则
var excludePackagePatternSet: Set<String>? = null
// 包含被注解的方法
var includeMethodAnnoSet: Set<String>? = null
// 排除被注解的方法
var excludeMethodAnnoSet: Set<String>? = null
// 包含被注解前缀的方法
var includeMethodAnnoPrefixSet: Set<String>? = null
// 排除被注解前缀的方法
var excludeMethodAnnoPrefixSet: Set<String>? = null
// 包含被注解的方法
var includeMethodAnnoPatternSet: Set<String>? = null
// 排除被注解的方法
var excludeMethodAnnoPatternSet: Set<String>? = null
```

> 添加 MethodTrace 的依赖库

```Kotlin
debugImplementation("com.github.vistring.AndroidMethodTrace:method-trace:<version>")
```

> 如果配置了 matchAll, 建议包含以下配置.

```Kotlin
vsMethodTraceConfig {
    // ......
    excludePackagePrefixSet = setOf(
        "kotlin.",
        "kotlinx.",
        "android.",
        "androidx.",
        "com.google.",
        // 如果还有其他的请提 issue 告诉我
    )
    excludeMethodAnnoPrefixSet = setOf(
        // 这个一定要配置. 否则对 @FromJson 的方法插桩会有运行错误
        // 如果还有其他的请提 issue 告诉我
        "com.squareup.moshi",
    )
    // ......
}
```

> 输出效果

请根据 "VSMethodTracker" 作为 tag 进行日志过滤

[点我请查看 log 文件](./traceLog.txt)
