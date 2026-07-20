package com.roboverse.fleet.api.dto;

import com.roboverse.fleet.domain.model.PageSlice;
import java.util.List;
import java.util.function.Function;

public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <S, T> PagedResponse<T> from(PageSlice<S> page, Function<S, T> mapper) {
        return new PagedResponse<>(page.content().stream().map(mapper).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}

