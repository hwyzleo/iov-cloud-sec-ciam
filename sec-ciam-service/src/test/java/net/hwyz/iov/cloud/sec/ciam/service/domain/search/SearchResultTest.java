package net.hwyz.iov.cloud.sec.ciam.service.domain.search;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchResultTest {

    @Test
    void empty_returnsZeroTotalAndEmptyItems() {
        SearchResult<String> result = SearchResult.empty(0, 10);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
    }

    @Test
    void builder_createsResultWithItems() {
        SearchResult<String> result = SearchResult.<String>builder()
                .items(List.of("a", "b", "c"))
                .total(100)
                .page(2)
                .size(3)
                .build();

        assertEquals(3, result.getItems().size());
        assertEquals(100, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(3, result.getSize());
    }

    @Test
    void empty_preservesPageAndSize() {
        SearchResult<Integer> result = SearchResult.empty(5, 25);

        assertEquals(5, result.getPage());
        assertEquals(25, result.getSize());
        assertEquals(0, result.getTotal());
    }
}
