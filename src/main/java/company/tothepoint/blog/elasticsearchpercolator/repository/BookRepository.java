package company.tothepoint.blog.elasticsearchpercolator.repository;

import company.tothepoint.blog.elasticsearchpercolator.domain.Book;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface BookRepository extends PagingAndSortingRepository<Book, String> {


}
