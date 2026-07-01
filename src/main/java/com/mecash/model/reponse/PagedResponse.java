package com.mecash.model.reponse;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable, explicit pagination envelope so we don't serialise Spring's {@code PageImpl}
 * (whose JSON shape is not part of its public contract).
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <S, T> PagedResponse<T> from(Page<S> page, List<T> mappedContent) {
        return new PagedResponse<>(
                mappedContent,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
