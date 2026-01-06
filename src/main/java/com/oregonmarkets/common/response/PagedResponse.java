package com.oregonmarkets.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic paginated response wrapper
 * Contains both the data and pagination metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResponse<T> {

    private List<T> content;
    private PaginationMetadata pagination;

    /**
     * Pagination metadata
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationMetadata {
        private int page;           // Current page number (0-indexed)
        private int size;           // Page size
        private long totalElements; // Total number of elements
        private int totalPages;     // Total number of pages
        private boolean first;      // Is this the first page?
        private boolean last;       // Is this the last page?
        private boolean empty;      // Is the page empty?

        /**
         * Create pagination metadata
         */
        public static PaginationMetadata of(int page, int size, long totalElements) {
            int totalPages = (int) Math.ceil((double) totalElements / size);

            return PaginationMetadata.builder()
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .first(page == 0)
                    .last(page >= totalPages - 1)
                    .empty(totalElements == 0)
                    .build();
        }
    }

    /**
     * Create a paged response
     */
    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        return PagedResponse.<T>builder()
                .content(content)
                .pagination(PaginationMetadata.of(page, size, totalElements))
                .build();
    }
}
