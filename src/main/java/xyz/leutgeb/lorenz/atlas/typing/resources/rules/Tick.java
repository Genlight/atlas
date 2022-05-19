package xyz.leutgeb.lorenz.atlas.typing.resources.rules;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import xyz.leutgeb.lorenz.atlas.ast.expressions.TickExpression;
import xyz.leutgeb.lorenz.atlas.typing.resources.AnnotatingGlobals;
import xyz.leutgeb.lorenz.atlas.typing.resources.proving.Obligation;

@Slf4j
public class Tick implements Rule {
  public static final Tick INSTANCE = new Tick();

  public ApplicationResult apply(
      Obligation obligation, AnnotatingGlobals globals, Map<String, String> arguments) {
    final var expression = obligation.getExpression();

    log.trace("Using (tick)!");

    if (expression instanceof TickExpression tickExpression) {
      if (!obligation.isCost()) {
        return ApplicationResult.onlyObligations(
            obligation.keepContextAndAnnotationAndCost(tickExpression.getBody()));
      }
      final var qPlusCost = obligation.getContext().getAnnotation();
      final var q = globals.getHeuristic().generate("tick" + qPlusCost.getName(), qPlusCost);

      return new ApplicationResult(
          List.of(
              new Obligation(
                  obligation.getContext().getIds(),
                  q,
                  tickExpression.getBody(),
                  obligation.getAnnotation(),
                  obligation.isCost(),
                  obligation.isCoin())),
          List.of(qPlusCost.increment(q, tickExpression.getCost(), "(tick)")));
    }

    if (!obligation.isCost()) {
      return ApplicationResult.noop(obligation);
    }

    final var annotation = obligation.getAnnotation();
    final var q = globals.getHeuristic().generate("tick" + annotation.getName(), annotation);
    log.warn(
        "Rule is not applied to a tick expression. Assuming a cost of 1 and continuing anyway.");
    return new ApplicationResult(
        List.of(obligation.keepCost(obligation.getContext(), expression, q)),
        List.of(q.increment(annotation, 1, "(tick)")));
  }
}
