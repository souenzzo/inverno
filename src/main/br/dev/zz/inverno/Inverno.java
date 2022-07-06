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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

record TextEncoderImpl(Context ctx) implements ProxyInstantiable {
    public Object newInstance(Value... arguments) {
        var encoding = StandardCharsets.UTF_8;
        var fields = new HashMap<String, Object>();
        var Uint8Array = ctx.getBindings("js").getMember("Uint8Array");
        // var Symbol = ctx.getBindings("js").getMember("Symbol");
        // Symbol.execute("encoding");
        fields.put("encoding", encoding.toString().toLowerCase());
        fields.put("encode", (ProxyExecutable) args -> {
            var x = args[0];
            var v = new LinkedList<>();
            for (var i : (Objects.isNull(x) ? "" : x.asString()).getBytes(encoding)) {
                v.add(i);
            }
            return Uint8Array.invokeMember("from", ProxyArray.fromList(v));
        });
        return ProxyObject.fromMap(fields);
    }
}

record FetchImpl(Context ctx, HttpClient http_client) implements  ProxyExecutable {

    public Object execute(Value... arguments) {

        var req = HttpRequest
                .newBuilder(URI.create(String.valueOf(arguments[0])))
                .build();
        var Promise = ctx.getBindings("js").getMember("Promise");
        var fut_res = http_client.sendAsync(
                req, HttpResponse.BodyHandlers.ofString()
        );
        return Promise.newInstance((ProxyExecutable) arguments1 -> {
            var ok = arguments1[0];
            fut_res.thenApply(res -> {
                var m = new HashMap<String, Object>();
                m.put("status", res.statusCode());
                ok.execute(ProxyObject.fromMap(m));
                return null;
            });
            return null;
        });
    }
}

record Btoa(Context ctx) implements ProxyExecutable {
    public Object execute(Value... arguments) {
        var encoder = Base64.getEncoder();
        var input = String.valueOf(arguments[0]).getBytes(StandardCharsets.UTF_8);
        return encoder.encodeToString(input);
    }
}

record Atob(Context ctx) implements ProxyExecutable {
    public Object execute(Value... arguments) {
        var decoder = Base64.getDecoder();
        var input = String.valueOf(arguments[0]);
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
        globalThis.putMember("btoa", new Btoa(ctx));
        globalThis.putMember("atob", new Atob(ctx));
        b.putMember("TextEncoder", new TextEncoderImpl(ctx));
        if (Objects.nonNull(http_client)) {
            b.putMember("fetch", new FetchImpl(ctx, http_client));
        }
        b.putMember("TextEncoder", new TextEncoderImpl(ctx));
    }
}
