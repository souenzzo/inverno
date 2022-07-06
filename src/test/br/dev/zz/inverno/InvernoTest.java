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
}
