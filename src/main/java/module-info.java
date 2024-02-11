module com.github.tonivade.purecfg {
  exports com.github.tonivade.purecfg;

  requires transitive com.github.tonivade.purefun;
  requires transitive com.github.tonivade.purefun.core;
  requires transitive com.github.tonivade.purefun.free;
  requires com.github.tonivade.purefun.monad;
  requires com.github.tonivade.purefun.effect;
  requires transitive com.github.tonivade.purefun.typeclasses;
  requires org.tomlj;
  requires transitive java.compiler;
}