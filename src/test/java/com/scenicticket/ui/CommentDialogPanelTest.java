package com.scenicticket.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommentDialogPanelTest {
    @Test
    void submissionContainsOnlyCommentBodyAndRating() {
        CommentDialogPanel panel = new CommentDialogPanel("拙政园");
        panel.setFormValues("园林很漂亮", 4);

        CommentDialogPanel.CommentSubmission submission = panel.submission();

        assertEquals("园林很漂亮", submission.content());
        assertEquals(4, submission.rating());
    }

    @Test
    void submissionKeepsClickTimeSnapshotWhenFormChangesLater() {
        CommentDialogPanel panel = new CommentDialogPanel("狮子林");
        panel.setFormValues("第一次内容", 5);

        CommentDialogPanel.CommentSubmission submission = panel.submission();
        panel.setFormValues("后来修改", 1);

        assertEquals("第一次内容", submission.content());
        assertEquals(5, submission.rating());
    }
}
