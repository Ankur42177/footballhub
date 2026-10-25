package com.footballhub.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Clamps client-supplied paging parameters so no request can ask for unbounded data. */
public final class Pagination {

    public static final int MAX_SIZE = 50;

    private Pagination() {
    }

    public static int page(Integer page) {
        return page == null || page < 0 ? 0 : Math.min(page, 10_000);
    }

    public static int size(Integer size, int defaultSize) {
        if (size == null || size < 1) {
            return defaultSize;
        }
        return Math.min(size, MAX_SIZE);
    }

    public static PageRequest of(Integer page, Integer size, int defaultSize, Sort sort) {
        return PageRequest.of(page(page), size(size, defaultSize), sort);
    }
}
