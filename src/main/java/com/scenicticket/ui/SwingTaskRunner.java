package com.scenicticket.ui;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import java.awt.Cursor;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class SwingTaskRunner {
    private static final String PROCESSING_SUFFIX = "\u5904\u7406\u4e2d...";
    private static final String DONE_SUFFIX = "\u5b8c\u6210";
    private static final String INTERRUPTED_SUFFIX = "\u5df2\u4e2d\u65ad";
    private static final String FAILED_SUFFIX = "\u5931\u8d25\uff1a";

    private final JFrame owner;
    private final JLabel statusLabel;
    private final SessionTaskGuard sessionTaskGuard;
    private final Consumer<Throwable> errorHandler;
    private int runningTasks;

    public SwingTaskRunner(JFrame owner, JLabel statusLabel, SessionTaskGuard sessionTaskGuard,
                           Consumer<Throwable> errorHandler) {
        this.owner = owner;
        this.statusLabel = statusLabel;
        this.sessionTaskGuard = sessionTaskGuard;
        this.errorHandler = errorHandler;
        owner.setGlassPane(UiComponents.busyGlassPane());
    }

    public <T> void run(String name, Callable<T> task, Consumer<T> onSuccess) {
        run(name, task, onSuccess, null);
    }

    public <T> void run(String name, Callable<T> task, Consumer<T> onSuccess, Consumer<String> onError) {
        long taskGeneration = sessionTaskGuard.currentToken();
        String processingStatus = name + PROCESSING_SUFFIX;
        statusLabel.setText(processingStatus);
        setBusy(true);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    T result = get();
                    if (!sessionTaskGuard.isCurrent(taskGeneration)) {
                        return;
                    }
                    onSuccess.accept(result);
                    if (processingStatus.equals(statusLabel.getText())) {
                        statusLabel.setText(name + DONE_SUFFIX);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (sessionTaskGuard.isCurrent(taskGeneration)) {
                        statusLabel.setText(name + INTERRUPTED_SUFFIX);
                    }
                } catch (ExecutionException e) {
                    handleFailure(name, taskGeneration, e.getCause() == null ? e : e.getCause(), onError);
                } catch (RuntimeException e) {
                    handleFailure(name, taskGeneration, e, onError);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void handleFailure(String name, long taskGeneration, Throwable throwable, Consumer<String> onError) {
        if (!sessionTaskGuard.isCurrent(taskGeneration)) {
            return;
        }
        String message = UiFormatters.chineseError(throwable);
        if (onError == null) {
            errorHandler.accept(throwable);
        } else {
            onError.accept(message);
        }
        statusLabel.setText(name + FAILED_SUFFIX + message);
    }

    private void setBusy(boolean busy) {
        runningTasks = Math.max(0, runningTasks + (busy ? 1 : -1));
        boolean active = runningTasks > 0;
        owner.getGlassPane().setVisible(active);
        owner.setCursor(active ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }
}
