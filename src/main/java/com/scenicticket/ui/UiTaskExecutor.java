package com.scenicticket.ui;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public interface UiTaskExecutor {
    <T> void run(String name, Callable<T> task, Consumer<T> onSuccess);
}
