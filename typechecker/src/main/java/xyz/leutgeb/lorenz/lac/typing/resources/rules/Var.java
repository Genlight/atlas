package xyz.leutgeb.lorenz.lac.typing.resources.rules;

import static xyz.leutgeb.lorenz.lac.util.Util.bug;

import xyz.leutgeb.lorenz.lac.ast.Identifier;
import xyz.leutgeb.lorenz.lac.typing.resources.AnnotatingGlobals;
import xyz.leutgeb.lorenz.lac.typing.resources.constraints.EqualityConstraint;
import xyz.leutgeb.lorenz.lac.typing.resources.proving.Obligation;
import xyz.leutgeb.lorenz.lac.typing.simple.types.TreeType;

public class Var implements Rule {
  public static final Var INSTANCE = new Var();

  public Rule.ApplicationResult apply(Obligation obligation, AnnotatingGlobals globals) {
    final var context = obligation.getContext();
    final var expression = obligation.getExpression();

    if (!(expression instanceof Identifier)) {
      throw bug("cannot apply (var) to expression that is not an identifier");
    }

    if (!(expression.getType() instanceof TreeType)) {
      if (!context.isEmpty()) {
        throw bug(
            "cannot apply (var) to identifier that is not of type tree with nonempty context");
      }
      return Rule.ApplicationResult.empty();
    }

    if (context.size() != 1) {
      throw bug("cannot apply (var) when context does not have exactly one element");
    }

    return Rule.ApplicationResult.onlyConstraints(
        EqualityConstraint.eq(context.getAnnotation(), obligation.getAnnotation(), "(var)"));
  }

  @Override
  public String getName() {
    return "var";
  }
}
