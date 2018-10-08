package company.tothepoint.blog.elasticsearchpercolator.domain;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

@Data
@Builder
@Document
public class Book {

    @Id
    private String bookId;

    @NotNull
    private String title;

    @NotNull
    private String isbn;

    @NotNull
    private String author;

    @NotNull
    private Double price;

    @NotNull
    private BookType type;

    @NotNull
    private BookLanguage language;
}
