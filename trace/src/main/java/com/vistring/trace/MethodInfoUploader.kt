package com.vistring.trace

import android.app.Application
import android.util.Log
import androidx.annotation.Keep
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.LinkedBlockingDeque

/**
 * 卡顿方法信息的上传
 */
internal object MethodInfoUploader {

    @Keep
    data class Task(
        val methodFlag: Int,
        val methodCost: Long,
        val methodTotalCost: Long,
        val stackTraceStr: String,
    )

    private val okhttpClient by lazy {
        OkHttpClient()
    }

    @Volatile
    private var application: Application? = null

    @Volatile
    private var appName: String = "Unknown"

    @Volatile
    private var methodTraceMappingFile: File? = null

    /**
     * 任务队列
     */
    private val taskQueue = LinkedBlockingDeque<Task>()

    private fun copyToFileFolderIfFileNotExist() {
        val localApplication = application ?: return
        Thread {
            runCatching {
                this.javaClass.classLoader?.getResourceAsStream("methodTraceMapping.txt")
                    ?.use { inputStream ->
                        methodTraceMappingFile =
                            File(
                                localApplication.filesDir,
                                "vsMethodTraceMapping.txt"
                            ).apply {
                                this.outputStream()
                                    .use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                            }
                    }
            }.onFailure {
                it.printStackTrace()
            }
        }.start()
    }

    @Throws(Exception::class)
    private fun updateMethodTraceInfo(
        methodCost: Long,
        methodTotalCost: Long,
        methodFullName: String,
        stackTraceStr: String,
    ) {
        val url = "http://192.168.100.40:8080/add"

        val response = okhttpClient.newCall(
            request = Request.Builder()
                .url(url)
                .post(
                    body = FormBody.Builder()
                        .add("appId", appName)
                        .add("methodCost", methodCost.toString())
                        .add("methodTotalCost", methodTotalCost.toString())
                        .add("methodFullName", methodFullName)
                        .add("stackTraceStr", stackTraceStr)
                        .build()
                )
                .build()
        ).execute()

        if (!response.isSuccessful) {
            throw Exception("Unexpected code $response")
        }

    }

    private fun startUploadLoop() {
        Thread {
            while (true) {
                val task = taskQueue.takeFirst()
                val localMethodTraceMappingFile = methodTraceMappingFile
                if (localMethodTraceMappingFile != null && localMethodTraceMappingFile.exists()) {
                    val methodSignature = MethodMapping.getByLineNumber(
                        randomAccessFile = RandomAccessFile(localMethodTraceMappingFile, "r"),
                        lineNumber = task.methodFlag,
                    )
                    Log.e(
                        MethodTracker.TAG,
                        "methodSignature = $methodSignature, methodTotalCost = ${task.methodTotalCost}, methodCost = ${task.methodCost}",
                    )
                    runCatching {
                        updateMethodTraceInfo(
                            methodCost = task.methodCost,
                            methodTotalCost = task.methodTotalCost,
                            methodFullName = methodSignature,
                            stackTraceStr = task.stackTraceStr,
                        )
                    }.onFailure {
                        Log.e(
                            MethodTracker.TAG,
                            "上传卡顿信息失败",
                            it,
                        )
                    }.onSuccess {
                        Log.d(
                            MethodTracker.TAG,
                            "上传卡顿信息成功",
                        )
                    }
                    task.stackTraceStr.split(";").forEach {
                        Log.e(
                            MethodTracker.TAG,
                            "\t\t$it",
                        )
                    }
                } else {
                    Log.d(
                        MethodTracker.TAG,
                        "localMethodTraceMappingFile 不存在",
                    )
                }
            }
        }.start()
    }

    /**
     * 添加的任务会在队列中, 并且根据 [Task.methodFlag] 在 methodMapping 中找到对应的方法签名信息
     *
     * [Task.stackTraceStr] 是一个字符串用 ; 分隔的堆栈信息
     */
    fun addUploadTask(
        task: Task,
    ) {
        taskQueue.add(task)
    }

    fun init(
        application: Application,
        appName: String,
    ) {
        if (this.application == null) {
            this.application = application
            this.appName = appName
            copyToFileFolderIfFileNotExist()
            startUploadLoop()
        }
    }

}