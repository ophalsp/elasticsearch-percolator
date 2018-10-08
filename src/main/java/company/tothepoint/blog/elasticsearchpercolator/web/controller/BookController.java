package company.tothepoint.blog.elasticsearchpercolator.web.controller;

import company.tothepoint.blog.elasticsearchpercolator.domain.Book;
import company.tothepoint.blog.elasticsearchpercolator.repository.BookRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static java.util.Optional.ofNullable;

@RestController
@RequestMapping(value = "/api/books", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookController {

    private final BookRepository bookRepository;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping
    public ResponseEntity<Iterable<Book>> findAll() {
        return ResponseEntity.ok(bookRepository.findAll());
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Book> findById(@PathVariable("id") String id) {
        return ofNullable(bookRepository.findOne(id))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Book> create(@Valid @RequestBody Book book) {
        return ResponseEntity.ok(bookRepository.save(book));
    }
}
