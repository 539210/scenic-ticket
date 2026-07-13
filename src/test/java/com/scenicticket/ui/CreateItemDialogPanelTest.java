package com.scenicticket.ui;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateItemDialogPanelTest {
    @Test
    void requestParsesCompleteItemFormAndFreezesValues() {
        CreateItemDialogPanel panel = new CreateItemDialogPanel(categories());
        panel.setFormValues("狮子林", 1, "苏州古典园林", "90.00", "15",
                " https://img/a.jpg\nhttps://img/b.jpg\nhttps://img/a.jpg ",
                "{\"source\":\"答辩录入\",\"level\":2}");

        CreateItemDialogPanel.CreateItemRequest request = panel.request();
        panel.setFormValues("后来修改", 0, "另一简介", "1", "0", "", "{}");

        assertEquals("狮子林", request.title());
        assertEquals(2L, request.categoryId());
        assertEquals("苏州古典园林", request.description());
        assertEquals(new BigDecimal("90.00"), request.price());
        assertEquals(new BigDecimal("15"), request.discountRate());
        assertEquals(List.of("https://img/a.jpg", "https://img/b.jpg"), request.images());
        assertEquals("答辩录入", request.metadata().getString("source"));
        assertEquals(2, request.metadata().getInteger("level"));
    }

    @Test
    void rejectsMissingCategoriesMalformedAmountsAndMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new CreateItemDialogPanel(Map.of()));

        CreateItemDialogPanel panel = new CreateItemDialogPanel(categories());
        panel.setFormValues("景点", 0, "简介", "not-money", "0", "", "{}");
        assertThrows(IllegalArgumentException.class, panel::request);

        panel.setFormValues("景点", 0, "简介", "80", "0", "", "{broken}");
        assertThrows(IllegalArgumentException.class, panel::request);
    }

    private static Map<Long, String> categories() {
        Map<Long, String> categories = new LinkedHashMap<>();
        categories.put(1L, "园林");
        categories.put(2L, "历史文化");
        return categories;
    }
}
