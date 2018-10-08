package company.tothepoint.blog.elasticsearchpercolator.web.controller;

import company.tothepoint.blog.elasticsearchpercolator.domain.SearchPreference;
import company.tothepoint.blog.elasticsearchpercolator.repository.SearchPreferenceRepository;
import company.tothepoint.blog.elasticsearchpercolator.service.BookstoreService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collection;

import static java.util.Optional.ofNullable;

@RestController
@RequestMapping(value = "/api/searchpreferences", produces = MediaType.APPLICATION_JSON_VALUE)
public class SearchPreferencesController {

    private final SearchPreferenceRepository searchPreferenceRepository;
    private final BookstoreService bookstoreService;

    public SearchPreferencesController(SearchPreferenceRepository searchPreferenceRepository,
                                       BookstoreService bookstoreService) {
        this.searchPreferenceRepository = searchPreferenceRepository;
        this.bookstoreService = bookstoreService;
    }

    @GetMapping
    public ResponseEntity<Iterable<SearchPreference>> findAll() {
        return ResponseEntity.ok(searchPreferenceRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SearchPreference> findById(@PathVariable("id") String id) {
        return ofNullable(searchPreferenceRepository.findOne(id))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SearchPreference> create(@Valid @RequestBody SearchPreference preference) throws Exception {
        return ResponseEntity.ok(bookstoreService.createSearchPreference(preference));
    }

    @GetMapping("find-matching-preferences/{bookId}")
    public ResponseEntity<Collection<SearchPreference>> findPreferencesThatMatchWithBook(
            @PathVariable("bookId") String bookId) throws Exception {
        return ResponseEntity.ok(bookstoreService.findMatchingPreferences(bookId));
    }
}
