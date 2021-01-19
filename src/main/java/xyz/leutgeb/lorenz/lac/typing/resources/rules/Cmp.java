package xyz.leutgeb.lorenz.lac.typing.resources.rules;

import xyz.leutgeb.lorenz.lac.typing.resources.AnnotatingGlobals;
import xyz.leutgeb.lorenz.lac.typing.resources.proving.Obligation;

import java.util.Map;

public class Cmp implements Rule {
  public static final Cmp INSTANCE = new Cmp();

  public Rule.ApplicationResult apply(Obligation obligation, AnnotatingGlobals globals, Map<String, String> arguments) {
    return Rule.ApplicationResult.empty();
  }

  @Override
  public String getName() {
    return "cmp";
  }
}
