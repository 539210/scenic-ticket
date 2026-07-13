package com.scenicticket.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommentDialogPanelTest {
    @Test
    void submissionNormalizesChineseAndEnglishTagSeparators() {
        CommentDialogPanel panel = new CommentDialogPanel("拙政园");
        panel.setFormValues("园林很漂亮", 4, "园林, 适合拍照，园林,  ");

        CommentDialogPanel.CommentSubmission submission = panel.submission();

        assertEquals("园林很漂亮", submission.content());
        assertEquals(4, submission.rating());
        assertEquals(List.of("园林", "适合拍照"), submission.tags());
    }

    @Test
    void submissionKeepsClickTimeSnapshotWhenFormChangesLater() {
        CommentDialogPanel panel = new CommentDialogPanel("狮子林");
        panel.setFormValues("第一次内容", 5, "亲子,园林");

        CommentDialogPanel.CommentSubmission submission = panel.submission();
        panel.setFormValues("后来修改", 1, "其他");

        assertEquals("第一次内容", submission.content());
        assertEquals(5, submission.rating());
        assertEquals(List.of("亲子", "园林"), submission.tags());
        assertThrows(UnsupportedOperationException.class, () -> submission.tags().add("篡改"));
    }
}
