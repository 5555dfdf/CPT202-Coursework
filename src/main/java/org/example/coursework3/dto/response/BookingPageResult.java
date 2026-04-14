package org.example.coursework3.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class BookingPageResult {
    private List<?> items;
    private Long total;
    private Integer page;
    private Integer pageSize;

    public static BookingPageResult of(List<?> items, long total, int page, int pageSize) {
        BookingPageResult result = new BookingPageResult();
        result.setItems(items);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        return result;
    }
}
