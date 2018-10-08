package company.tothepoint.blog.elasticsearchpercolator;

import company.tothepoint.blog.elasticsearchpercolator.config.PercolatorIndexFields;
import company.tothepoint.blog.elasticsearchpercolator.domain.Book;
import company.tothepoint.blog.elasticsearchpercolator.domain.BookLanguage;
import company.tothepoint.blog.elasticsearchpercolator.domain.BookType;
import company.tothepoint.blog.elasticsearchpercolator.domain.SearchPreference;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static company.tothepoint.blog.elasticsearchpercolator.config.ElasticsearchConfig.PERCOLATOR_INDEX;
import static company.tothepoint.blog.elasticsearchpercolator.config.ElasticsearchConfig.PERCOLATOR_INDEX_MAPPING_TYPE;
import static company.tothepoint.blog.elasticsearchpercolator.config.PercolatorIndexFields.PERCOLATOR_QUERY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SearchPreferenceControllerTest extends AbstractIntegrationTestCase {

    @After
    public void cleanup() {
        SearchResponse searchResponse = elasticSearchClient.prepareSearch(PERCOLATOR_INDEX)
                .get();

        for (SearchHit hit : searchResponse.getHits()) {
            elasticSearchClient.prepareDelete()
                    .setId(hit.getId())
                    .setType(PERCOLATOR_INDEX_MAPPING_TYPE)
                    .setIndex(PERCOLATOR_INDEX)
                    .get();
        }
    }


    @Test
    public void testFindAll() throws Exception {
        SearchPreference cheapBooks = SearchPreference.builder()
                .title("Find me some cheap books")
                .email("peter.ophals@tothepoint.company")
                .criteria(SearchPreference.Criteria.builder()
                        .maximumPrice(9.99)
                        .build())
                .build();
        searchPreferenceRepository.save(cheapBooks);

        SearchPreference stiegLarsonBooks = SearchPreference.builder()
                .title("Find me some Stieg Larson' books")
                .email("peter.ophals@tothepoint.company")
                .criteria(SearchPreference.Criteria.builder()
                        .author("Stieg Larson")
                        .build())
                .build();
        searchPreferenceRepository.save(stiegLarsonBooks);

        MvcResult result = doGet("/api/searchpreferences");
        List<SearchPreference> response = Arrays.asList(mapper.readValue(result.getResponse().getContentAsString(), SearchPreference[].class));
        assertThat(response.size()).isEqualTo(2);
    }

    @Test
    public void testFindOne() throws Exception {
        SearchPreference cheapBooks = SearchPreference.builder()
                .title("Find me some cheap books")
                .email("peter.ophals@tothepoint.company")
                .criteria(SearchPreference.Criteria.builder()
                        .maximumPrice(9.99)
                        .build())
                .build();
        searchPreferenceRepository.save(cheapBooks);

        MvcResult result = doGet("/api/searchpreferences/" + cheapBooks.getSearchPreferenceId());
        SearchPreference response = mapper.readValue(result.getResponse().getContentAsString(), SearchPreference.class);
        assertThat(response.getTitle()).isEqualTo("Find me some cheap books");
    }

    @Test
    public void testFindNotExisting() throws Exception {
        doGetWithExpectedStatus("/api/searchpreferences/-12", status().isNotFound());
    }

    @Test
    public void testCreateSearchPreference() throws Exception {
        SearchPreference newPreference = SearchPreference.builder()
                .title("Find me some ramdom books")
                .email("peter.ophals@tothepoint.company")
                .criteria(SearchPreference.Criteria.builder()
                        .author("A random preference author")
                        .language(BookLanguage.ENGLISH)
                        .types(new BookType[]{BookType.NONFICTION})
                        .minimumPrice(10.00)
                        .maximumPrice(100.00)
                        .build())
                .build();

        MvcResult result = mockMvc.perform(post("/api/searchpreferences")
                .content(mapper.writeValueAsString(newPreference))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isNotEmpty();

        SearchPreference searchPreference = mapper.readValue(result.getResponse().getContentAsString(), SearchPreference.class);
        assertThat(searchPreference.getSearchPreferenceId()).isNotEmpty();

        SearchPreference aSavedSearchPreference = searchPreferenceRepository.findOne(searchPreference.getSearchPreferenceId());
        assertThat(aSavedSearchPreference).isNotNull();
        assertThat(aSavedSearchPreference.getTitle()).isEqualTo("Find me some ramdom books");
        assertThat(aSavedSearchPreference.getEmail()).isEqualTo("peter.ophals@tothepoint.company");
        assertThat(aSavedSearchPreference.getCriteria().getAuthor()).isEqualTo("A random preference author");
        assertThat(aSavedSearchPreference.getCriteria().getTypes()).isEqualTo(new BookType[]{BookType.NONFICTION});
        assertThat(aSavedSearchPreference.getCriteria().getLanguage()).isEqualTo(BookLanguage.ENGLISH);
        assertThat(aSavedSearchPreference.getCriteria().getMinimumPrice()).isEqualTo(10.00);
        assertThat(aSavedSearchPreference.getCriteria().getMaximumPrice()).isEqualTo(100.00);

        GetRequest getRequest = new GetRequest()
                .id(searchPreference.getSearchPreferenceId())
                .index(PERCOLATOR_INDEX);
        GetResponse elasticSearchData = elasticSearchClient.get(getRequest).actionGet();
        assertThat(elasticSearchData).isNotNull();
        Map<String, Object> indexSource = elasticSearchData.getSource();
        assertThat(indexSource).isNotNull();
        assertThat(indexSource).containsKeys("query");
    }

    @Test
    public void testMatchingPreferencesForBook() throws Exception {
        //GIVEN
        SearchPreference cheapBooks = SearchPreference.builder()
                .title("Find me some cheap books")
                .email("peter.ophals@tothepoint.company")
                .criteria(SearchPreference.Criteria.builder()
                        .maximumPrice(9.99)
                        .build())
                .build();
        searchPreferenceRepository.save(cheapBooks);

        elasticSearchClient.prepareIndex(PERCOLATOR_INDEX, PERCOLATOR_INDEX_MAPPING_TYPE, cheapBooks.getSearchPreferenceId())
                .setSource(jsonBuilder()
                        .startObject()
                        .field(PERCOLATOR_QUERY.getFieldName(), QueryBuilders.boolQuery().filter(
                                QueryBuilders.rangeQuery(PercolatorIndexFields.PRICE.getFieldName())
                                        .lte(9.99))) // Register the query
                        .endObject())
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE) // Needed when the query shall be available immediately
                .get();


        SearchPreference stiegLarsonBooks = SearchPreference.builder()
                .title("Find me some fancy Stieg Larson' books")
                .email("peter.ophals@tothepoint.company")
                .criteria(SearchPreference.Criteria.builder()
                        .author("Stieg Larson")
                        .minimumPrice(10.99)
                        .build())
                .build();
        searchPreferenceRepository.save(stiegLarsonBooks);

        elasticSearchClient.prepareIndex(PERCOLATOR_INDEX, PERCOLATOR_INDEX_MAPPING_TYPE, stiegLarsonBooks.getSearchPreferenceId())
                .setSource(jsonBuilder()
                        .startObject()
                        .field(PERCOLATOR_QUERY.getFieldName(),
                                QueryBuilders.boolQuery()
                                        .filter(QueryBuilders.termsQuery(PercolatorIndexFields.AUTHOR.getFieldName(), "Stieg Larson"))
                                        .filter(QueryBuilders.rangeQuery(PercolatorIndexFields.PRICE.getFieldName())
                                                .gte(10.99)))
                        .endObject())
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE) // Needed when the query shall be available immediately
                .get();

        Book savedBook = bookRepository.save(
                Book.builder()
                        .author("Stieg Larson")
                        .title("Some title")
                        .isbn("549545465")
                        .language(BookLanguage.ENGLISH)
                        .type(BookType.FICTION)
                        .price(15.99)
                        .build());

        //WHEN
        MvcResult result = doGet("/api/searchpreferences/find-matching-preferences/" + savedBook.getBookId());
        List<SearchPreference> response = Arrays.asList(mapper.readValue(result.getResponse().getContentAsString(), SearchPreference[].class));

        //THEN
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.get(0).getTitle()).isEqualTo("Find me some fancy Stieg Larson' books");

    }

    @Test
    public void testMatchingPreferencesForNotExistingBookId() throws Exception {
        MvcResult result = doGet("/api/searchpreferences/find-matching-preferences/-12");
        List<SearchPreference> response = Arrays.asList(mapper.readValue(result.getResponse().getContentAsString(), SearchPreference[].class));
        assertThat(response).isEmpty();
    }
}
