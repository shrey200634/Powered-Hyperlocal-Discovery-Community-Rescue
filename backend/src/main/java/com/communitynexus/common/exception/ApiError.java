package com.communitynexus.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)   // hide "field" when null
public record ApiError(String code, String message, String field) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }
}