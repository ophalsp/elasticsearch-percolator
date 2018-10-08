package company.tothepoint.blog.elasticsearchpercolator.config;

import lombok.Getter;

@Getter
public enum PercolatorIndexFields {

    PERCOLATOR_QUERY("query", "percolator"),
    EMAIL("email", "keyword"),
    TITLE("title", "keyword"),
    AUTHOR("author", "keyword"),
    PRICE("sellingPrice", "double"),
    TYPE("bookType", "keyword"),
    LANGUAGE("bookLanguage", "keyword");

    private final String fieldName;
    private final String fieldType;

    PercolatorIndexFields(String fieldName, String fieldType) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

}
