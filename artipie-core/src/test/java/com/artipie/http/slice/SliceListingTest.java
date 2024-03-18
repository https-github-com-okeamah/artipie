/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.json.Json;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Test case for {@link SliceListingTest}.
 */
class SliceListingTest {

    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.storage.save(new Key.From("target0.txt"), new Content.Empty()).join();
        this.storage.save(new Key.From("one/target1.txt"), new Content.Empty()).join();
        this.storage.save(new Key.From("one/two/target2.txt"), new Content.Empty()).join();
    }

    @ParameterizedTest
    @CsvSource({
        "not-exists/,''",
        "one/,'one/target1.txt\none/two/target2.txt'"
    })
    void responseTextType(final String path, final String body) {
        MatcherAssert.assertThat(
            new SliceListing(this.storage, "text/plain", ListingFormat.Standard.TEXT)
                .response(rqLineFrom(path), Headers.EMPTY, Flowable.empty()),
            new ResponseMatcher(
                RsStatus.OK,
                Arrays.asList(
                    new Header(
                        "Content-Type",
                        String.format("text/plain; charset=%s", StandardCharsets.UTF_8)
                    ),
                    new Header("Content-Length", String.valueOf(body.length()))
                ),
                body.getBytes(StandardCharsets.UTF_8)
            )
        );
    }

    @Test
    void responseJsonType() {
        final String json = Json.createArrayBuilder(
            Arrays.asList("one/target1.txt", "one/two/target2.txt")
        ).build().toString();
        MatcherAssert.assertThat(
            new SliceListing(this.storage, "application/json", ListingFormat.Standard.JSON)
                .response(rqLineFrom("one/"), Headers.EMPTY, Flowable.empty()),
            new ResponseMatcher(
                RsStatus.OK,
                Arrays.asList(
                    new Header("Content-Type", "application/json; charset=" + StandardCharsets.UTF_8),
                    new Header("Content-Length", String.valueOf(json.length()))
                ),
                json.getBytes(StandardCharsets.UTF_8)
            )
        );
    }

    @Test
    void responseHtmlType() {
        final String body = String.join(
            "\n",
            "<!DOCTYPE html>",
            "<html>",
            "  <head><meta charset=\"utf-8\"/></head>",
            "  <body>",
            "    <ul>",
            "      <li><a href=\"one/target1.txt\">one/target1.txt</a></li>",
            "      <li><a href=\"one/two/target2.txt\">one/two/target2.txt</a></li>",
            "    </ul>",
            "  </body>",
            "</html>"
        );
        MatcherAssert.assertThat(
            new SliceListing(this.storage, "text/html", ListingFormat.Standard.HTML)
                .response(rqLineFrom("/one"), Headers.EMPTY, Flowable.empty()),
            new ResponseMatcher(
                RsStatus.OK,
                Arrays.asList(
                    new Header(
                        "Content-Type",
                        String.format("text/html; charset=%s", StandardCharsets.UTF_8)
                    ),
                    new Header("Content-Length", String.valueOf(body.length()))
                ),
                body.getBytes(StandardCharsets.UTF_8)
            )
        );
    }

    private static RequestLine rqLineFrom(final String path) {
        return new RequestLine("GET", path, "HTTP/1.1");
    }
}
