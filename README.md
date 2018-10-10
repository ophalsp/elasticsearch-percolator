# Matching documents with queries using the Elasticsearch Percolator query

## Introduction
Recently, on one of my projects, a real estate website, a new story popped-up telling us that we should use the search preferences (price, type of real estate, location), that registered users can save, to send notifications to these users, when a new house or appartement is published on the website.

Not immediately having a clue how to solve this problem in a timely and proper way I started searching on the Internet.  Maybe it's my Googling skills, but no luck finding anything on the first attempt.  

After a discussion with one of my colleagues he pointed my to Elasticsearch and something called a percolator query.  My first thought, what in the world has coffee to do with my problem?  No problem, I like coffee, so I went for one and then I started browsing the Elasticsearch website.

## So what is a percolator query?
A percolate query is a query, allowing us to match a document with queries stored in an index.  So this means that we can store a lot of queries in a Elatisticsearch index, with a special type, and afterwards we can give a document to that index and ask which queries match our document.

**Let me try to make it more clear using an example in a bookstore context:**

As a user of an online bookstore I regularly search for FICTION books, written in English, that cost between 15 and 30 euro, these search criteria are then stored, linked to my account.  In my example a query in elasticsearch would look something like this:
```json
{
  "bool": {
    "filter": [
      {
        "terms": {
          "bookType": [
            "FICTION"
          ],
          "boost": 1
        }
      },
      {
        "terms": {
          "bookLanguage": [
            "ENGLISH"
          ],
          "boost": 1
        }
      },
      {
        "range": {
          "sellingPrice": {
            "from": 15,
            "to": 30,
            "include_lower": true,
            "include_upper": true,
            "boost": 1
          }
        }
      }
    ],
    "adjust_pure_negative": true,
    "boost": 1
  }
}
```

This query can be stored in an index in Elasticsearch, with a mapping that looks like this:
```json
{
  "mapping": {
    "docs": {
      "properties": {
        "author": {
          "type": "keyword"
        },
        "bookLanguage": {
          "type": "keyword"
        },
        "bookType": {
          "type": "keyword"
        },
        "query": {
          "type": "percolator"
        },
        "sellingPrice": {
          "type": "double"
        }
      }
    }
  }
}
```

When a new book is then published on the website the possibility exists to match this book against the different queries already store in our index.  For example the following book:
```json
{
	"title":"The Girl Who Played with Fire",
	"isbn":"1234567890",
	"author":"Stieg Larson",
	"price":"18.99",
	"type":"FICTION",
	"language":"ENGLISH"
}
```

If I would wrap this book in a perolate query, it would match my previously saved search preference.

## How did I implement this?
I've made a example project that can be found on : https://github.com/ophalsp/elasticsearch-percolator, using Java, Spring Boot and the Elasticsearch Java API.

To run it locally, you should have Docker installed and be able to run docker-compose.  Then clone the project to your disk, navigate to the folder and execute the following commands:
- ``` docker-compose up ```
- ``` ./mvwn spring-boot:run ``` for Linux/Mac OS X or ``` mvnw.bat spring-boot:run ``` if you're on Windows

The books and the search preferences metadata is stored in a Mongo database that is launched with the docker-compose up command.

### First step: create the index
Multiple options are possible, you could create the index manually by sending a payload to a REST endpoint on the elastic search server, I opted for creating the index in my application.

```java
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
```

Using the PostConstruct annotation that comes with Spring Boot, I initialise the index after dependency injection is terminated and before any other action is performed.

Before creating the index I check if not exists already, would I try to create the index multiple times, each time my application restarts, it would throw a  ```ResourceAlreadyExistsException```

The PercolatorIndexFields class is just an enum containing the fields used for the mapping, allowing me to define my fieldnames in one central location, since I will be needing them on multiple places.  It looks like this:
```java
public enum PercolatorIndexFields {

    PERCOLATOR_QUERY("query", "percolator"),
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
```
### Second step: insert a query in the index
To facilitate I've created some REST endpoints where you can upload some data to this service.  The first one is ```http://localhost:8080/api/searchpreferences ```.  You can send a POST request with the following json payload using Postman or whatever way you want:
```json
{
	"title" : "I like cheap ENGLISH non-fiction books",
	"email": "your-email@your.company",
	"criteria":{
		"language": "ENGLISH",
		"types": ["FICTION"],
		"minimumPrice": 15.00,
		"maximumPrice": 30.00
	}
}
```

This will create the index using the following code:
```java
BoolQueryBuilder bqb = QueryBuilders.boolQuery();

if (preference.getCriteria().getAuthor() != null) {
    bqb.filter(QueryBuilders.termsQuery(PercolatorIndexFields.AUTHOR.getFieldName(), preference.getCriteria().getAuthor()));
}

if (preference.getCriteria().getTypes() != null) {
    bqb.filter(QueryBuilders.termsQuery(PercolatorIndexFields.TYPE.getFieldName(), preference.getCriteria().getTypes()));
}

if (preference.getCriteria().getLanguage() != null) {
    bqb.filter(QueryBuilders.termsQuery(PercolatorIndexFields.LANGUAGE.getFieldName(), preference.getCriteria().getLanguage()));
}

if (preference.getCriteria().getMinimumPrice() != null && preference.getCriteria().getMaximumPrice() != null) {
    bqb.filter(
            QueryBuilders.rangeQuery(PercolatorIndexFields.PRICE.getFieldName())
                    .gte(preference.getCriteria().getMinimumPrice().doubleValue())
                    .lte(preference.getCriteria().getMaximumPrice().doubleValue()));
} else if (preference.getCriteria().getMinimumPrice() != null) {
    bqb.filter(
            QueryBuilders.rangeQuery(PercolatorIndexFields.PRICE.getFieldName())
                    .gte(preference.getCriteria().getMinimumPrice().doubleValue()));
} else if (preference.getCriteria().getMaximumPrice() != null) {
    bqb.filter(QueryBuilders.rangeQuery(PercolatorIndexFields.PRICE.getFieldName())
            .lte(preference.getCriteria().getMaximumPrice().doubleValue()));
}

elasticsearchClient.prepareIndex(PERCOLATOR_INDEX, PERCOLATOR_INDEX_MAPPING_TYPE, savedPreference.getSearchPreferenceId())
        .setSource(jsonBuilder()
                .startObject()
                .field(PercolatorIndexFields.PERCOLATOR_QUERY.getFieldName(), bqb) // Register the query
                .endObject())
        .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE) // Needed when the query shall be available immediately
        .get();
```

### Third step: upload a book
Send a POST request to ```http://localhost:8080/api/books``` with an payload like this:
```json 
{
	"title":"The Girl Who Played with Fire",
	"isbn":"1234567890",
	"author":"Stieg Larson",
	"price":"18.99",
	"type":"FICTION",
	"language":"ENGLISH"
}
```

In the result payload you will get a bookId, keep it nearby, you'll need it in the following step.

### Last step: match your book with the existing queries
Now of course we would like to see if the system works, try and send a request to the following endpoint ``` ``` where you translate {bookId} with the id of the book you've created in the previous step.

The code achieving this looks like this:
```java 
XContentBuilder docBuilder = XContentFactory.jsonBuilder().startObject();
docBuilder.field(PercolatorIndexFields.AUTHOR.getFieldName(), book.getAuthor());
docBuilder.field(PercolatorIndexFields.LANGUAGE.getFieldName(), book.getLanguage().name());
docBuilder.field(PercolatorIndexFields.PRICE.getFieldName(), book.getPrice());
docBuilder.field(PercolatorIndexFields.TYPE.getFieldName(), book.getType());
docBuilder.endObject();

PercolateQueryBuilder percolateQuery = new PercolateQueryBuilder(PercolatorIndexFields.PERCOLATOR_QUERY.getFieldName(),
        BytesReference.bytes(docBuilder),
        XContentType.JSON);

// Percolate, by executing the percolator query in the query dsl:
SearchResponse searchResponse = elasticsearchClient.prepareSearch(PERCOLATOR_INDEX)
        .setQuery(percolateQuery)
        .execute()
        .actionGet();
```

So what I do here is:
- I construct a document using the data from the book I've fetched from the Mongo database using the correct fieldnames
- I then put this document in a ```PercolateQueryBuilder``` object
- I search my percolator index with this ```PercolateQueryBuilder``` object

If everything went well, you should now see a result containing your searchpreference with title : 'I like cheap ENGLISH non-fiction books'

## What about unit/integration testing this?
Obviously I also wanted to write tests for this.  But how should I do this?  Do unit test suffise?  Or should I also try to do a full integration test?  
On my real project I should do both, but since this is only an example project, I only implemented the integration test.

Regarding the integration with Mongo I had no problems, a flapdoodle library exists to have a embedded Mongo when running doing integration testing in Spring.

But what about Elasticsearch, there is no such library.  So I've searched the Internet and many examples showed that you could run an Elasticsearch node using a ``` nodeBuilder()``` method in the Elastichsearch Java API.  Unfortunately, in my version 6.4.0 there is no such method.  So back to the Internet, which explained that the ```Node``` class would allow the same functionality.

Nothing is more true.  When using this Node class the first problem that you encounter is ```Unsupported transport.type```.  I tried to configure multiple properties but nothing helped, until I came up with the following solution:
```java
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
```
I've created a new class extending ```Node```, this gives you the opportunity to pass a list of plugins to a protected constructor in the ```Node``` class.  Which is the key to resolving the ```Unsupported transport.type``` problem.

When I combine this with the ```@BeforeClass``` and ```@AfterClass``` when running my tests, I have now a local elasticsearch node running on which I can perform my tests :
```java 
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
```


### Conclusion
Honestly I can say that this percolator query feature in Elasticsearch is a powerfull means to an end.  Without it I'm not quite sure how I would have resolved my initial problem.  It was not without an effort that I came up with this final solution, but it gave me a lot of satisfaction at the end.

I hope I have gave you a better understanding of what this percolate query functionality is.  If you have any remarks/suggestions, please feel free.  I'm always open to discuss, try to help you or improve my own knowledge if you have a better solution.