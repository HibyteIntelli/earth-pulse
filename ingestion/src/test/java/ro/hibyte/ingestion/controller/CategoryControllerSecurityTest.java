package ro.hibyte.ingestion.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.hibyte.ingestion.config.SecurityConfig;
import ro.hibyte.ingestion.dto.response.CategoryResponse;
import ro.hibyte.ingestion.service.CategoryService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class CategoryControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void isPublicNoAuthRequired() throws Exception {
        when(categoryService.getCategories()).thenReturn(List.of(
                new CategoryResponse("wildfires", "Wildfires")));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk());
    }
}
