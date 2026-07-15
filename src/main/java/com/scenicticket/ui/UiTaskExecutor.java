package com.scenicticket.ui;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public interface UiTaskExecutor {
    <T> void run(String name, Callable<T> task, Consumer<T> onSuccess);

    default <T> void run(String name, Callable<T> task, Consumer<T> onSuccess, Consumer<String> onError) {
        run(name, task, onSuccess);
    }

    /** Runs non-blocking prefetch work without taking over the whole window when supported. */
    default <T> void runQuietly(String name, Callable<T> task, Consumer<T> onSuccess) {
        run(name, task, onSuccess);
    }

    default <T> void runQuietly(String name, Callable<T> task, Consumer<T> onSuccess, Consumer<String> onError) {
        run(name, task, onSuccess, onError);
    }
}
