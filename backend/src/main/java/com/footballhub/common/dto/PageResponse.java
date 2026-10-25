package com.footballhub.common.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/** Consistent pagination envelope for every collection endpoint. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages,
        boolean hasNext) {

    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.hasNext());
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) ((totalElements + size - 1) / size);
        boolean hasNext = (long) (page + 1) * size < totalElements;
        return new PageResponse<>(content, page, size, totalElements, totalPages, hasNext);
    }
}
