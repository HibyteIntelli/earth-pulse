package ro.hibyte.ingestion.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.hibyte.ingestion.dto.response.CategoryResponse;
import ro.hibyte.ingestion.repository.EventRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void mapsKnownIdToHumanTitle() {
        when(eventRepository.findDistinctCategoryIds()).thenReturn(List.of("wildfires"));

        List<CategoryResponse> result = categoryService.getCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("wildfires");
        assertThat(result.get(0).getTitle()).isEqualTo("Wildfires");
    }

    @Test
    void echoesUnknownIdAsTitle() {
        when(eventRepository.findDistinctCategoryIds()).thenReturn(List.of("mysteryCategory"));

        List<CategoryResponse> result = categoryService.getCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("mysteryCategory");
        assertThat(result.get(0).getTitle()).isEqualTo("mysteryCategory");
    }

    @Test
    void sortsCategoryIdsAlphabetically() {
        when(eventRepository.findDistinctCategoryIds())
                .thenReturn(List.of("wildfires", "earthquakes", "floods"));

        List<CategoryResponse> result = categoryService.getCategories();

        assertThat(result)
                .extracting(CategoryResponse::getId)
                .containsExactly("earthquakes", "floods", "wildfires");
    }
}
