package company.tothepoint.blog.elasticsearchpercolator;

import company.tothepoint.blog.elasticsearchpercolator.domain.Book;
import company.tothepoint.blog.elasticsearchpercolator.domain.BookLanguage;
import company.tothepoint.blog.elasticsearchpercolator.domain.BookType;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BookControllerTest extends AbstractIntegrationTestCase {

    @Test
    public void testFindAll() throws Exception {
        Book jungleBook = Book.builder()
                .author("Rudyard Kipling")
                .title("Jungle Book")
                .isbn("1234667890")
                .language(BookLanguage.ENGLISH)
                .type(BookType.FICTION)
                .price(15.99)
                .build();
        bookRepository.save(jungleBook);

        Book matilda = Book.builder()
                .author("Roald Dahl")
                .title("Matilda")
                .isbn("0987654321")
                .language(BookLanguage.ENGLISH)
                .type(BookType.FICTION)
                .price(18.99)
                .build();
        bookRepository.save(matilda);

        MvcResult result = doGet("/api/books");
        List<Book> response = Arrays.asList(mapper.readValue(result.getResponse().getContentAsString(), Book[].class));
        assertThat(response.size()).isEqualTo(2);
    }

    @Test
    public void testFindOne() throws Exception {
        Book jungleBook = Book.builder()
                .author("Rudyard Kipling")
                .title("Jungle Book")
                .isbn("1234667890")
                .language(BookLanguage.ENGLISH)
                .type(BookType.FICTION)
                .price(15.99)
                .build();
        bookRepository.save(jungleBook);

        MvcResult result = doGet("/api/books/" + jungleBook.getBookId());
        Book response = mapper.readValue(result.getResponse().getContentAsString(), Book.class);
        assertThat(response.getIsbn()).isEqualTo("1234667890");
    }

    @Test
    public void testFindNotExisting() throws Exception {
        doGetWithExpectedStatus("/api/books/-12", status().isNotFound());
    }

    @Test
    public void testCreateBook() throws Exception {
        Book newBook = Book.builder()
                .author("Some author")
                .title("Some title")
                .isbn("549545465")
                .language(BookLanguage.ENGLISH)
                .type(BookType.FICTION)
                .price(15.99)
                .build();

        MvcResult result = mockMvc.perform(post("/api/books")
                .content(mapper.writeValueAsString(newBook))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isNotEmpty();

        Book book = mapper.readValue(result.getResponse().getContentAsString(), Book.class);
        assertThat(book.getBookId()).isNotEmpty();

        Book savedBook = bookRepository.findOne(book.getBookId());
        assertThat(savedBook).isNotNull();
        assertThat(savedBook.getTitle()).isEqualTo("Some title");
        assertThat(savedBook.getAuthor()).isEqualTo("Some author");
        assertThat(savedBook.getIsbn()).isEqualTo("549545465");
        assertThat(savedBook.getPrice()).isEqualTo(15.99);
        assertThat(savedBook.getType()).isEqualTo(BookType.FICTION);
        assertThat(savedBook.getLanguage()).isEqualTo(BookLanguage.ENGLISH);
    }
}
