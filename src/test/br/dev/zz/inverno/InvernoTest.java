package br.dev.zz.inverno;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public record InvernoTest() {
    @Test
    void TextEncoderTest() {
        try (var ctx = Context.create()) {
            Inverno.wintercg(ctx);
            assertEquals(
                    "Uint8Array(3)[226, 130, 172]",
                    ctx.eval("js", "(new TextEncoder()).encode('€')").toString()
            );
        }
    }
}
