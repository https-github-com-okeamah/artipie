/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.artipie.http;

import com.artipie.Settings;
import com.artipie.api.ArtipieApi;
import com.artipie.dashboard.DashboardSlice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.RtPath;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.LoggingSlice;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Pie of slices.
 * @since 0.1
 * @checkstyle ReturnCountCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class Pie extends Slice.Wrap {

    /**
     * Route path returns {@code NO_CONTENT} status if path is empty.
     */
    private static final RtPath EMPTY_PATH = (line, headers, body) -> {
        final String path = new RequestLineFrom(line).uri().getPath();
        final Optional<Response> res;
        if (path.equals("*") || path.equals("/")
            || path.replaceAll("^/+", "").split("/").length == 0) {
            res = Optional.of(new RsWithStatus(RsStatus.NO_CONTENT));
        } else {
            res = Optional.empty();
        }
        return res;
    };

    /**
     * Artipie entry point.
     * @param settings Artipie settings
     */
    public Pie(final Settings settings) {
        super(
            new SafeSlice(
                new LoggingSlice(
                    Level.INFO,
                    new SliceRoute(
                        Pie.EMPTY_PATH,
                        new RtRulePath(
                            new RtRule.ByPath(Pattern.compile("/api/?.*")), new ArtipieApi(settings)
                        ),
                        new RtRulePath(
                            new RtIsDashboard(settings), new DashboardSlice(settings)
                        ),
                        new RtRulePath(
                            RtRule.FALLBACK, new DockerRoutingSlice(new SliceByPath(settings))
                        )
                    )
                )
            )
        );
    }
}
