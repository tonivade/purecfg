module com.github.tonivade.purecfg {
  exports com.github.tonivade.purecfg;

  requires transitive com.github.tonivade.purefun;
  requires transitive com.github.tonivade.purefun.core;
  requires transitive com.github.tonivade.purefun.free;
  requires transitive com.github.tonivade.purefun.monad;
  requires transitive com.github.tonivade.purefun.typeclasses;
  requires transitive org.tomlj;
  requires transitive java.compiler;
}