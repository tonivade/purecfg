/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purecfg;

import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple3;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Validation;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static com.github.tonivade.purecfg.PureCFG.map3;
import static com.github.tonivade.purecfg.PureCFG.readBoolean;
import static com.github.tonivade.purecfg.PureCFG.readConfig;
import static com.github.tonivade.purecfg.PureCFG.readInt;
import static com.github.tonivade.purecfg.PureCFG.readIterable;
import static com.github.tonivade.purecfg.PureCFG.readString;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PureCFGTest {

  @Test
  void run() {
    PureCFG<Config> cfg = program();

    Properties properties = new Properties();
    properties.put("server.host", "localhost");
    properties.put("server.port", "8080");
    properties.put("server.active", "true");
    Source source = Source.fromProperties(properties);

    assertAll(
        () -> assertConfig(cfg.unsafeRun(source)),
        () -> assertConfig(cfg.safeRun(source).get()),
        () -> assertConfig(cfg.validatedRun(source).get())
    );
  }

  @Test
  void iterable() {
    PureCFG<Iterable<String>> iterable = readIterable("list", readString("it"));

    Properties properties = new Properties();
    properties.put("list.0.it", "a");
    properties.put("list.1.it", "b");
    properties.put("list.2.it", "c");

    Option<Iterable<String>> option = iterable.safeRun(Source.fromProperties(properties));

    assertAll(
        () -> assertEquals(listOf("a", "b", "c"), option.get()),
        () -> assertEquals("- list.[].it: String\n", iterable.describe())
    );
  }

  @Test
  void analyzeListOf() {
    PureCFG<Iterable<Tuple3<String, Integer, Boolean>>> iterable =
        readIterable("list", map3(readString("a"), readInt("b"), readBoolean("c"), Tuple::of));

    assertEquals("- list.[].a: String\n- list.[].b: Integer\n- list.[].c: Boolean\n", iterable.describe());
  }

  @Test
  void error() {
    PureCFG<Config> cfg = program();

    Properties properties = new Properties();
    Source source = Source.fromProperties(properties);

    assertAll(
        () -> assertThrows(NullPointerException.class, () -> cfg.unsafeRun(source)),
        () -> assertEquals(Option.none(), cfg.safeRun(source)),
        () -> assertEquals(
            Validation.invalid(
                Validation.Result.of(
                    "key not found: server.active",
                    "key not found: server.port",
                    "key not found: server.host")),
            cfg.validatedRun(source))
    );
  }

  @Test
  void analyze() {
    PureCFG<Config> program = program();

    String result = program.describe();

    assertEquals("- server.host: String\n- server.port: Integer\n- server.active: Boolean\n", result);
  }

  private PureCFG<Config> program() {
    PureCFG<String> host = readString("host");
    PureCFG<Integer> port = readInt("port");
    PureCFG<Boolean> active = readBoolean("active");

    PureCFG<Config> hostAndPort = map3(host, port, active, Config::new);

    return readConfig("server", hostAndPort);
  }

  private void assertConfig(Config config) {
    assertEquals("localhost", config.getHost());
    assertEquals(8080, config.getPort());
    assertTrue(config.isActive());
  }
}

final class Config {

  private final String host;
  private final int port;
  private final boolean active;

  Config(String host, int port, boolean active) {
    this.host = host;
    this.port = port;
    this.active = active;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public boolean isActive() {
    return active;
  }

  @Override
  public String toString() {
    return "Config{" +
        "host='" + host + '\'' +
        ", port=" + port +
        ", active=" + active +
        '}';
  }
}