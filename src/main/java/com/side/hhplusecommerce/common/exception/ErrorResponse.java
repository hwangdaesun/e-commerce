package com.side.hhplusecommerce.common.exception;

public record ErrorResponse(
        String code,
        String message
) {
}
