/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.misc.UncheckedConsumer;
import com.artipie.test.TestArtipieCaches;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.nio.charset.StandardCharsets;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link RolesRest}.
 * @since 0.27
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
final class RolesRestTest extends RestApiServerBase {

    @Test
    void listsRoles(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.saveIntoSecurityStorage(
            new Key.From("roles/java-dev.yaml"),
            String.join(
                System.lineSeparator(),
                "permissions:",
                "  adapter_basic_permission:",
                "    maven:",
                "      - write",
                "      - read"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.saveIntoSecurityStorage(
            new Key.From("roles/readers.yml"),
            String.join(
                System.lineSeparator(),
                "permissions:",
                "  adapter_basic_permission:",
                "    \"*\":",
                "      - read"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/roles"),
            new UncheckedConsumer<>(
                response -> JSONAssert.assertEquals(
                    response.body().toString(),
                    // @checkstyle LineLengthCheck (1 line)
                    "[{\"name\":\"java-dev\",\"permissions\":{\"adapter_basic_permission\":{\"maven\":[\"write\",\"read\"]}}},{\"name\":\"readers\",\"permissions\":{\"adapter_basic_permission\":{\"*\":[\"read\"]}}}]",
                    false
                )
            )
        );
    }

    @Test
    void getsRole(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.saveIntoSecurityStorage(
            new Key.From("roles/java-dev.yaml"),
            String.join(
                System.lineSeparator(),
                "permissions:",
                "  adapter_basic_permission:",
                "    maven:",
                "      - write",
                "      - read"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/roles/java-dev"),
            new UncheckedConsumer<>(
                response -> JSONAssert.assertEquals(
                    response.body().toString(),
                    // @checkstyle LineLengthCheck (1 line)
                    "{\"name\":\"java-dev\",\"permissions\":{\"adapter_basic_permission\":{\"maven\":[\"write\",\"read\"]}}}",
                    false
                )
            )
        );
    }

    @Test
    void returnsNotFoundIfRoleDoesNotExist(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/roles/testers"),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.NOT_FOUND_404)
            )
        );
    }

    @Test
    void altersRole(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.saveIntoSecurityStorage(
            new Key.From("roles/testers.yaml"),
            String.join(
                System.lineSeparator(),
                "permissions:",
                "  adapter_basic_permission:",
                "    test-repo:",
                "      - write",
                "      - read"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT, "/api/v1/roles/testers",
                new JsonObject().put(
                    "permissions", new JsonObject().put(
                        "adapter_basic_permission",
                        new JsonObject().put("test-maven", JsonArray.of("read"))
                            .put("test-pypi", JsonArray.of("r", "w"))
                    )
                )
            ),
            response -> {
                MatcherAssert.assertThat(
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.CREATED_201)
                );
                MatcherAssert.assertThat(
                    new String(
                        this.securityStorage().value(new Key.From("roles/testers.yaml")),
                        StandardCharsets.UTF_8
                    ),
                    new IsEqual<>(
                        String.join(
                            System.lineSeparator(),
                            "permissions:",
                            "  adapter_basic_permission:",
                            "    \"test-maven\":",
                            "      - read",
                            "    \"test-pypi\":",
                            "      - r",
                            "      - w"
                        )
                    )
                );
            }
        );
    }

    @Test
    void addsRole(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT, "/api/v1/roles/java-dev",
                new JsonObject().put(
                    "permissions",
                    new JsonObject().put(
                        "adapter_basic_permissions",
                        new JsonObject().put("maven-repo", JsonArray.of("read", "write"))
                    )
                )
            ),
            response -> {
                MatcherAssert.assertThat(
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.CREATED_201)
                );
                MatcherAssert.assertThat(
                    new String(
                        this.securityStorage().value(new Key.From("roles/java-dev.yml")),
                        StandardCharsets.UTF_8
                    ),
                    new IsEqual<>(
                        String.join(
                            System.lineSeparator(),
                            "permissions:",
                            "  adapter_basic_permissions:",
                            "    \"maven-repo\":",
                            "      - read",
                            "      - write"
                        )
                    )
                );
                MatcherAssert.assertThat(
                    "Policy cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wasPolicyInvalidated()
                );
            }
        );
    }

    @Test
    void returnsNotFoundIfRoleDoesNotExistOnDelete(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.DELETE, "/api/v1/roles/any"),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.NOT_FOUND_404)
            )
        );
    }

    @Test
    void returnsNotFoundIfRoleDoesNotExistOnEnable(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.POST, "/api/v1/roles/tester/enable"),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.NOT_FOUND_404)
            )
        );
    }

    @Test
    void returnsNotFoundIfRoleDoesNotExistOnDisable(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.POST, "/api/v1/roles/admin/disable"),
            response -> MatcherAssert.assertThat(
                response.statusCode(),
                new IsEqual<>(HttpStatus.NOT_FOUND_404)
            )
        );
    }

    @Test
    void removesRole(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.saveIntoSecurityStorage(
            new Key.From("roles/devs.yaml"),
            new byte[]{}
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.DELETE, "/api/v1/roles/devs"),
            response -> {
                MatcherAssert.assertThat(
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    this.securityStorage().exists(new Key.From("roles/devs.yaml")),
                    new IsEqual<>(false)
                );
                MatcherAssert.assertThat(
                    "Policy cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wasPolicyInvalidated()
                );
            }
        );
    }

    @Test
    void enablesRole(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.saveIntoSecurityStorage(
            new Key.From("roles/java-dev.yml"),
            String.join(
                System.lineSeparator(),
                "enabled: false",
                "permissions:",
                "  adapter_basic_permissions:",
                "    \"maven-repo\":",
                "      - read",
                "      - write"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.POST, "/api/v1/roles/java-dev/enable"),
            response -> {
                MatcherAssert.assertThat(
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    new String(
                        this.securityStorage().value(new Key.From("roles/java-dev.yml")),
                        StandardCharsets.UTF_8
                    ),
                    new IsEqual<>(
                        String.join(
                            System.lineSeparator(),
                            "enabled: true",
                            "permissions:",
                            "  adapter_basic_permissions:",
                            "    \"maven-repo\":",
                            "      - read",
                            "      - write"
                        )
                    )
                );
                MatcherAssert.assertThat(
                    "Policy cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wasPolicyInvalidated()
                );
            }
        );
    }

    @Test
    void disablesRole(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.saveIntoSecurityStorage(
            new Key.From("roles/java-dev.yml"),
            String.join(
                System.lineSeparator(),
                "permissions:",
                "  adapter_basic_permissions:",
                "    \"maven-repo\":",
                "      - read",
                "      - write"
            ).getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.POST, "/api/v1/roles/java-dev/disable"),
            response -> {
                MatcherAssert.assertThat(
                    response.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    new String(
                        this.securityStorage().value(new Key.From("roles/java-dev.yml")),
                        StandardCharsets.UTF_8
                    ),
                    new IsEqual<>(
                        String.join(
                            System.lineSeparator(),
                            "permissions:",
                            "  adapter_basic_permissions:",
                            "    \"maven-repo\":",
                            "      - read",
                            "      - write",
                            "enabled: false"
                        )
                    )
                );
                MatcherAssert.assertThat(
                    "Policy cache should be invalidated",
                    ((TestArtipieCaches) this.settingsCaches()).wasPolicyInvalidated()
                );
            }
        );
    }

    @Override
    String layout() {
        return "org";
    }
}