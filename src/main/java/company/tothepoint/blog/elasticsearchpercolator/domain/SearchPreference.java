package company.tothepoint.blog.elasticsearchpercolator.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Builder
@Document
public class SearchPreference {

    @Id
    private String searchPreferenceId;

    @NotNull
    private String title;

    @NotNull
    private String email;

    @NotNull
    @Valid
    private Criteria criteria;

    @Value
    @Builder
    public static class Criteria {
        private String author;

        private Double minimumPrice;

        private Double maximumPrice;

        private BookType[] types;

        private BookLanguage language;
    }
}
