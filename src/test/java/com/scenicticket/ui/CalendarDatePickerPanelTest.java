package com.scenicticket.ui;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalendarDatePickerPanelTest {
    @Test
    void navigatingTwelveMonthsChangesYearAndKeepsMonthSelectable() {
        CalendarDatePickerPanel panel = new CalendarDatePickerPanel(LocalDate.of(2026, 7, 15));

        for (int month = 0; month < 12; month++) {
            panel.nextMonth();
        }
        panel.selectDay(20);

        assertEquals(YearMonth.of(2027, 7), panel.getDisplayedMonth());
        assertEquals(LocalDate.of(2027, 7, 20), panel.getSelectedDate());
    }

    @Test
    void previousMonthCrossesYearBoundary() {
        CalendarDatePickerPanel panel = new CalendarDatePickerPanel(LocalDate.of(2026, 1, 3));

        panel.previousMonth();
        panel.selectDay(31);

        assertEquals(YearMonth.of(2025, 12), panel.getDisplayedMonth());
        assertEquals(LocalDate.of(2025, 12, 31), panel.getSelectedDate());
    }
}
