package com.side.hhplusecommerce.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CursorRequest {
    private Long cursor;
    private Integer size;

    public static CursorRequest of(Long cursor, Integer size) {
        return new CursorRequest(cursor, size);
    }
}