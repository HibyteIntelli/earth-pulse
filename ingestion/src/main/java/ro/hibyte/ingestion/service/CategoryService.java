package ro.hibyte.ingestion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.hibyte.ingestion.dto.request.CategoryEnum;
import ro.hibyte.ingestion.dto.response.CategoryResponse;
import ro.hibyte.ingestion.repository.EventRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final EventRepository eventRepository;

    public List<CategoryResponse> getCategories(){
        return eventRepository.findDistinctCategoryIds().stream()
                .sorted()
                .map(id -> new CategoryResponse(
                        id,
                        CategoryEnum.fromValue(id).map(CategoryEnum::getTitle).orElse(id)))
                .toList();
    }
}
