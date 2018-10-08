package company.tothepoint.blog.elasticsearchpercolator.repository;

import company.tothepoint.blog.elasticsearchpercolator.domain.SearchPreference;
import org.springframework.data.repository.CrudRepository;

public interface SearchPreferenceRepository extends CrudRepository<SearchPreference, String> {
}
