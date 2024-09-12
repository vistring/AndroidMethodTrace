package com.vistring.trace.demo.test1;

import androidx.annotation.CheckResult;
import androidx.annotation.Keep;
import androidx.collection.LongSet;

import org.jetbrains.annotations.Nullable;

public class TestJava {

    @Nullable
    @CheckResult
    public static LongSet longSetOf(long var0, long var2) {
        return null;
    }

    @Keep
    @Nullable
    public static LongSet longSetOf1(long var0, long var2) {
        return longSetOf(
                var0,
                var2
        );
    }

}
