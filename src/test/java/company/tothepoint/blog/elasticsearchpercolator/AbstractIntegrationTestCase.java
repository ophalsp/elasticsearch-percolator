package company.tothepoint.blog.elasticsearchpercolator;

import com.fasterxml.jackson.databind.ObjectMapper;
import company.tothepoint.blog.elasticsearchpercolator.repository.BookRepository;
import company.tothepoint.blog.elasticsearchpercolator.repository.SearchPreferenceRepository;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.reindex.ReindexPlugin;
import org.elasticsearch.join.ParentJoinPlugin;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.percolator.PercolatorPlugin;
import org.elasticsearch.script.mustache.MustachePlugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ElasticsearchPercolatorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AbstractIntegrationTestCase {

    @Autowired
    protected BookRepository bookRepository;

    @Autowired
    protected SearchPreferenceRepository searchPreferenceRepository;

    @Autowired
    protected Client elasticSearchClient;

    @Autowired
    protected ObjectMapper mapper;

    @Autowired
    protected MockMvc mockMvc;

    private static Node esNode;

    @BeforeClass
    public static final void startEs() throws NodeValidationException {
        Settings settings = Settings.builder()
                .put("cluster.name", "bookstore-test-cluster")
                .put("path.home", "target/es")
                .build();
        esNode = new LocalNode(settings);
        esNode.start();
    }

    @AfterClass
    public static final void stopEs() throws IOException {
        if (esNode != null && !esNode.isClosed()) {
            esNode.close();
        }
    }

    @Before
    public final void clearDatabase() {
        bookRepository.deleteAll();
        searchPreferenceRepository.deleteAll();
    }

    @Test
    public void testContextLoads(){

    }

    private static class LocalNode extends Node {
        private static final String ES_WORKING_DIR = "target/es";

        public LocalNode(Settings settings) {
            super(new Environment(settings, Paths.get(ES_WORKING_DIR)),
                    Collections.unmodifiableList(
                            Arrays.asList(
                                    Netty4Plugin.class,
                                    ReindexPlugin.class,
                                    PercolatorPlugin.class,
                                    MustachePlugin.class,
                                    ParentJoinPlugin.class)));
        }

    }

    protected MvcResult doGet(String url) throws Exception {
        return mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
    }

    protected MvcResult doGetWithExpectedStatus(String url, ResultMatcher statusResultMatcher) throws Exception {
        return mockMvc.perform(get(url))
                .andExpect(statusResultMatcher)
                .andReturn();
    }
}
