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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

record TextEncoderImpl(Context ctx) implements ProxyInstantiable {
    public Object newInstance(Value... arguments) {
        var encoding = StandardCharsets.UTF_8;
        var fields = new HashMap<String, Object>();
        Value Uint8Array = ctx.getBindings("js").getMember("Uint8Array");
        Value Symbol = ctx.getBindings("js").getMember("Symbol");
        // Symbol.execute("encoding");
        fields.put("encoding", encoding.toString().toLowerCase());
        fields.put("encode", (ProxyExecutable) args -> {
            var x = args[0];
            var v = new LinkedList<>();
            for (var i:  (Objects.isNull(x) ? "" : x.asString()).getBytes(encoding)) {
                v.add(i);
            }
            return Uint8Array.invokeMember("from",
                    (Object) ProxyArray.fromList(v));
        });
        return ProxyObject.fromMap(fields);
    }
}

record FetchImpl(Context ctx, HttpClient http_client) implements  ProxyExecutable {

    public Object execute(Value... arguments) {

        var req = URI.create(String.valueOf(arguments[0]));
        Value Promise = ctx.getBindings("js").getMember("Promise");
        return Promise.newInstance(new ProxyExecutable() {
            public Object execute(Value... arguments) {
                var ok = arguments[0];
                if (Objects.nonNull(ok) && ok.canExecute()) {
                    ok.execute(42);
                }
                return null;
            }
        });
    }
}
public record Inverno() {
    static public void wintercg(Context ctx) {
        wintercg(ctx, null);
    }
    static public void wintercg(Context ctx, HttpClient http_client) {
        var b = ctx.getBindings("js");
        b.putMember("TextEncoder", new TextEncoderImpl(ctx));
        if (Objects.nonNull(http_client)) {
            b.putMember("fetch", new FetchImpl(ctx, http_client));
        }
        // b.putMember("TextEncoder", new TextEncoderImpl(ctx));
    }
}
