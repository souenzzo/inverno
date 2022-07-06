package br.dev.zz.inverno;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

public record InvernoTest() {
    static {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
    }

    @Test
    void TextEncoderTest() {
        try (var ctx = Context.create()) {
            Inverno.wintercg(ctx);
            ctx.eval("js", "let te = new TextEncoder()");
            assertEquals(
                    "Uint8Array(3)[226, 130, 172]",
                    ctx.eval("js", "te.encode('€')").toString()
            );
        }
    }

    @Test
    void FetchTest() throws InterruptedException {
        try (var ctx = Context.create()) {
            var reqs = new LinkedList<HttpRequest>();
            Inverno.wintercg(ctx, new HttpClient() {
                public Optional<CookieHandler> cookieHandler() {
                    return Optional.empty();
                }

                public Optional<Duration> connectTimeout() {
                    return Optional.empty();
                }

                public Redirect followRedirects() {
                    return null;
                }

                public Optional<ProxySelector> proxy() {
                    return Optional.empty();
                }

                public SSLContext sslContext() {
                    return null;
                }

                public SSLParameters sslParameters() {
                    return null;
                }

                public Optional<Authenticator> authenticator() {
                    return Optional.empty();
                }

                public Version version() {
                    return null;
                }

                public Optional<Executor> executor() {
                    return Optional.empty();
                }

                public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
                    return null;
                }

                public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
                    reqs.add(request);
                    return MinimalFuture.completedFuture(new HttpResponse<T>() {
                        public int statusCode() {
                            return 200;
                        }

                        public HttpRequest request() {
                            return request;
                        }

                        public Optional<HttpResponse<T>> previousResponse() {
                            return Optional.empty();
                        }

                        public HttpHeaders headers() {
                            return null;
                        }

                        public T body() {
                            return null;
                        }

                        @Override
                        public Optional<SSLSession> sslSession() {
                            return Optional.empty();
                        }

                        @Override
                        public URI uri() {
                            return request.uri();
                        }

                        @Override
                        public Version version() {
                            return request.version().orElse(null);
                        }
                    });
                }

                public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
                    return null;
                }
            });
            ctx.eval("js", "fetch('https://souenzzo.com.br').then(result => foo = result)");
            Thread.sleep(100);
            assertEquals(
                    "42",
                    ctx.eval("js", "foo").toString()
            );
        }
    }
}
