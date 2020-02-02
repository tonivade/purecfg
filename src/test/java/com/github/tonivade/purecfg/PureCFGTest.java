package com.github.tonivade.purecfg;

import com.github.tonivade.purefun.data.NonEmptyString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static com.github.tonivade.purecfg.PureCFG.readInt;
import static com.github.tonivade.purecfg.PureCFG.readString;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PureCFGTest {

  @Test
  void test() {
    PureCFG<String> host = readString(NonEmptyString.of("server.host"));
    PureCFG<Integer> port = readInt(NonEmptyString.of("server.port"));

    PureCFG<Config> cfg = PureCFG.map2(host, port, Config::new);

    Properties properties = new Properties();
    properties.put("server.host", "localhost");
    properties.put("server.port", "8080");
    Config config = cfg.fromProperties(properties);

    assertEquals("localhost", config.getHost());
    assertEquals(8080, config.getPort());
  }
}

final class Config {
  private final String host;
  private final Integer port;

  Config(String host, Integer port) {
    this.host = host;
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public Integer getPort() {
    return port;
  }

  @Override
  public String toString() {
    return "Config{" +
        "host='" + host + '\'' +
        ", port=" + port +
        '}';
  }
}