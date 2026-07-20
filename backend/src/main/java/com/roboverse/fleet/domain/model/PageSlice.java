package com.roboverse.fleet.domain.model;

import java.util.List;

public record PageSlice<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public PageSlice {
        content = List.copyOf(content);
    }
}

