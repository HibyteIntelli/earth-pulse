package ro.hibyte.notifier.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import ro.hibyte.notifier.entity.CategoryEnum;

@Component
public class CategoryEnumConverter implements Converter<String, CategoryEnum> {

    @Override
    public CategoryEnum convert(String source) {
        return CategoryEnum.fromValue(source)
                .orElseThrow(() -> new IllegalArgumentException("Unknown category: " + source));
    }
}
