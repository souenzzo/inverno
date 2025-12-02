package br.dev.zz.inverno;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

record TextEncoderImpl(Context ctx, Value uint8array) implements ProxyInstantiable {
    public TextEncoderImpl(Context ctx) {
        this(ctx, ctx.getBindings("js").getMember("Uint8Array"));
    }
    public Object newInstance(Value... arguments) {
        var encoding = StandardCharsets.UTF_8;
        return ProxyObject.fromMap(Map.of(
                "encoding", encoding.toString().toLowerCase(),
                "encode", (ProxyExecutable) args -> {
                    var x = args[0];
                    var v = new LinkedList<>();
                    for (var i : (Objects.isNull(x) ? "" : x.asString()).getBytes(encoding)) {
                        v.add(i);
                    }
                    return uint8array.invokeMember("from", ProxyArray.fromList(v));
                }));
    }
}

record FetchImpl(Context ctx, HttpClient http_client) implements ProxyExecutable {

    public Object execute(Value... arguments) {

        var req = HttpRequest
                .newBuilder(URI.create(arguments[0].asString()))
                .build();
        var b = ctx.getBindings("js");
        var promise = b.getMember("Promise");
        var error = b.getMember("Error");
        var fut_res = http_client.sendAsync(
                req, HttpResponse.BodyHandlers.ofString()
        );
        return promise.newInstance((ProxyExecutable) arguments1 -> {
            var resolve = arguments1[0];
            var reject = arguments1[1];
            fut_res.thenApply(res -> {
                resolve.execute(ProxyObject.fromMap(Map.of("status", res.statusCode())));
                return null;
            }).exceptionally(throwable -> reject.execute(error.execute(throwable.getMessage())));
            return null;
        });
    }
}

record Btoa(Context ctx) implements ProxyExecutable {
    public Object execute(Value... arguments) {
        var encoder = Base64.getEncoder();
        var input = arguments[0].asString().getBytes(StandardCharsets.UTF_8);
        return encoder.encodeToString(input);
    }
}

record Atob(Context ctx) implements ProxyExecutable {
    public Object execute(Value... arguments) {
        var decoder = Base64.getDecoder();
        var input = arguments[0].asString();
        return new String(decoder.decode(input), StandardCharsets.UTF_8);
    }
}


public record Inverno() {
    static public void wintercg(Context ctx) {
        wintercg(ctx, null);
    }

    static public void wintercg(Context ctx, HttpClient http_client) {
        var b = ctx.getBindings("js");
        var globalThis = b.getMember("globalThis");
        globalThis.putMember("self", globalThis);
        globalThis.putMember("btoa", new Btoa(ctx));
        globalThis.putMember("atob", new Atob(ctx));
        b.putMember("TextEncoder", new TextEncoderImpl(ctx));
        if (Objects.nonNull(http_client)) {
            b.putMember("fetch", new FetchImpl(ctx, http_client));
        }
    }
}
