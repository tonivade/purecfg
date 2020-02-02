package com.github.tonivade.purecfg;

import com.github.tonivade.purefun.data.NonEmptyString;
import org.junit.jupiter.api.Test;

import static com.github.tonivade.purecfg.PureCFG.readInt;
import static com.github.tonivade.purecfg.PureCFG.readString;

class PureCFGTest {

  @Test
  void test() {
    PureCFG<String> host = readString(NonEmptyString.of("server.host"));
    PureCFG<Integer> port = readInt(NonEmptyString.of("server.port"));

    PureCFG<Config> cfg = PureCFG.map2(host, port, Config::new);

    System.out.println(cfg);
  }

}

final class Config {
  private final String host;
  private final Integer port;

  Config(String host, Integer port) {
    this.host = host;
    this.port = port;
  }
}