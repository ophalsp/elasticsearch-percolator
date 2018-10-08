package company.tothepoint.blog.elasticsearchpercolator.config;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;

@Slf4j
@Configuration
public class ElasticsearchConfig {

    public static final String PERCOLATOR_INDEX = "percolator_index";
    public static final String PERCOLATOR_INDEX_MAPPING_TYPE = "docs";


    @Value("${company.tothepoint.blog.elastic.cluster.name}")
    private String clusterName;
    @Value("${company.tothepoint.blog.elastic.host.url}")
    private String elasticHostUrl;
    @Value("${company.tothepoint.blog.elastic.host.port}")
    private int elasticHostPort;

    @Bean
    public Client elasticsearchClient() throws Exception {
        Settings settings = Settings.builder()
                .put("cluster.name", clusterName)
                .build();
        Client transportClient = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(elasticHostUrl), elasticHostPort));
        return transportClient;
    }

    /**
     * Create the index for the percolator data if it does not exist
     */
    @PostConstruct
    public void initializePercolatorIndex() {
        try {
            Client client = elasticsearchClient();

            IndicesExistsResponse indicesExistsResponse = client.admin().indices().prepareExists(PERCOLATOR_INDEX).get();

            if (indicesExistsResponse == null || !indicesExistsResponse.isExists()) {
                XContentBuilder percolatorQueriesMapping = XContentFactory.jsonBuilder()
                        .startObject()
                        .startObject("properties");

                Arrays.stream(PercolatorIndexFields.values())
                        .forEach(field -> {
                            try {
                                percolatorQueriesMapping
                                        .startObject(field.getFieldName())
                                        .field("type", field.getFieldType())
                                        .endObject();
                            } catch (IOException e) {
                                log.error(String.format("Error while adding field %s to mapping", field.name()), e);
                                throw new RuntimeException(
                                        String.format("Something went wrong while adding field %s to mapping", field.name()), e);
                            }
                        });

                percolatorQueriesMapping
                        .endObject()
                        .endObject();

                client.admin().indices().prepareCreate(PERCOLATOR_INDEX)
                        .addMapping(PERCOLATOR_INDEX_MAPPING_TYPE, percolatorQueriesMapping)
                        .execute()
                        .actionGet();
            }
        } catch (Exception e) {
            log.error("Error while creating percolator index", e);
            throw new RuntimeException("Something went wrong during the creation of the percolator index", e);
        }
    }
}