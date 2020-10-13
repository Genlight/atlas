package xyz.leutgeb.lorenz.lac.typing.resources;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.Token;
import xyz.leutgeb.lorenz.lac.antlr.TacticBaseVisitor;
import xyz.leutgeb.lorenz.lac.antlr.TacticParser;
import xyz.leutgeb.lorenz.lac.typing.resources.coefficients.Coefficient;
import xyz.leutgeb.lorenz.lac.typing.resources.coefficients.KnownCoefficient;
import xyz.leutgeb.lorenz.lac.typing.resources.constraints.EqualityConstraint;
import xyz.leutgeb.lorenz.lac.typing.resources.proving.Obligation;
import xyz.leutgeb.lorenz.lac.typing.resources.proving.Prover;
import xyz.leutgeb.lorenz.lac.util.Fraction;

@Value
@Slf4j
public class TacticVisitorImpl extends TacticBaseVisitor<Object> {
  Prover prover;
  Obligation obligation;

  private void proveInternal(
      Obligation obligation, TacticParser.TacticExpressionContext tacticExpression) {
    if (tacticExpression
        instanceof TacticParser.NamedTacticExpressionContext annotatedTacticExpression) {
      final String annotation = annotatedTacticExpression.name.getText();
      tacticExpression = annotatedTacticExpression.tacticExpression();
      if (annotation != null) {
        prover.record(annotation, obligation);
      }
    }

    Token start = null;
    if (tacticExpression instanceof TacticParser.FixedAnnotationContext fixedAnnotationContext) {
      start = fixedAnnotationContext.getStart();
      Optional<Annotation> optionalFromFixing =
          convert(obligation.getContext().getAnnotation().size(), fixedAnnotationContext.from);
      Optional<Annotation> optionalToFixing =
          convert(obligation.getAnnotation().size(), fixedAnnotationContext.to);

      if (optionalFromFixing.isPresent()) {
        log.info(
            "Fixing annotation named '{}' on line {}: {} = {}",
            obligation.getContext().getAnnotation().getNameAndId(),
            fixedAnnotationContext.getStart().getLine(),
            obligation.getContext().getAnnotation(),
            optionalFromFixing.get());
        prover.addExternalConstraints(
            EqualityConstraint.eq(
                optionalFromFixing.get(),
                obligation.getContext().reorderLexicographically().getAnnotation(),
                "(tactic) fixed at position "
                    + start.getLine()
                    + ":"
                    + start.getCharPositionInLine()
                    + " (indices mean lexicographically reordered context)"));
      }

      if (optionalToFixing.isPresent()) {
        log.info(
            "Fixing annotation named '{}' on line {}: {} = {}",
            obligation.getAnnotation().getNameAndId(),
            fixedAnnotationContext.getStart().getLine(),
            obligation.getAnnotation(),
            optionalToFixing.get());
        prover.addExternalConstraints(
            EqualityConstraint.eq(
                optionalToFixing.get(),
                obligation.getAnnotation(),
                "(tactic) fixed at position "
                    + start.getLine()
                    + ":"
                    + start.getCharPositionInLine()));
      }

      proveInternal(obligation, fixedAnnotationContext.next);
      return;
    }

    String ruleNameText;
    List<TacticParser.TacticExpressionContext> next;
    if (tacticExpression instanceof TacticParser.TerminalTacticExpressionContext terminal) {
      ruleNameText = terminal.identifier.getText();
      start = terminal.getStart();
      next = emptyList();
    } else if (tacticExpression
        instanceof TacticParser.ListTacticExpressionContext listTacticExpressionContext) {
      start = listTacticExpressionContext.getStart();
      ruleNameText = listTacticExpressionContext.elements.get(0).getText();
      next =
          listTacticExpressionContext.elements.subList(
              1, listTacticExpressionContext.elements.size());
    } else {
      throw new UnsupportedOperationException();
    }
    if (ruleNameText.equals("?")) {
      log.warn(
          "Leaving hole at position {}:{} for obligation {}",
          start.getLine(),
          start.getCharPositionInLine(),
          obligation);
      return;
    }

    Obligation weakened = prover.weakenVariables(obligation);

    long count = 0;
    List<Obligation> result;
    if (ruleNameText.startsWith("_")) {
      if (!ruleNameText.contains("auto")) {
        log.info(
            "Expanding {} at position {}:{}",
            ruleNameText,
            start.getLine(),
            start.getCharPositionInLine());
        prover.setLogApplications(true);
      }
      prover.setAuto(ruleNameText.contains("auto"));
      prover.setTreeCf(ruleNameText.contains("cf"));
      prover.setWeakenBeforeTerminal(ruleNameText.contains("w"));
      prover.prove(obligation);
      if (!ruleNameText.contains("auto")) {
        prover.setLogApplications(false);
      }
      result = emptyList();
    } else {
      result = prover.applyByName(ruleNameText, weakened);
      count = result.stream().filter(x -> x.getCost() != 0).count();
    }

    if (count != next.size()) {
      throw new RuntimeException(
          "Given tactic does not apply: Rule ("
              + ruleNameText
              + ") applied to \n\n\t\t"
              + obligation
              + "\n\n yields "
              + result.size()
              + " new obligations but "
              + next.size()
              + " are covered.");
    }
    for (int i = 0; i < result.size(); i++) {
      if (result.get(i).getCost() == 0) {
        prover.prove(result.get(i));
      } else {
        proveInternal(result.get(i), next.get(i));
      }
    }
  }

  @Override
  public Object visitTactic(TacticParser.TacticContext ctx) {
    proveInternal(obligation, ctx.tacticExpression());
    return null;
  }

  private Optional<Annotation> convert(int size, TacticParser.AnnotationContext annotationContext) {
    // if (annotationContext instanceof TacticParser.DontCareAnnotationContext) {
    //	return Optional.empty();
    // }
    if (annotationContext instanceof TacticParser.ZeroAnnotationContext) {
      final var start = annotationContext.getStart();
      return Optional.of(
          Annotation.zero(
              size, "fixed at position " + start.getLine() + ":" + start.getCharPositionInLine()));
    }
    if (annotationContext instanceof TacticParser.NonEmptyAnnotationContext context) {
      List<Coefficient> rankCoefficients = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        rankCoefficients.add(KnownCoefficient.ZERO);
      }
      Map<List<Integer>, Coefficient> coeffiecients = new HashMap<>();
      for (var entry : context.entries) {
        final var index = entry.index();
        final var value = convert(entry.coefficient);
        if (index instanceof TacticParser.RankIndexContext rankIndex) {
          rankCoefficients.set(Integer.parseInt(rankIndex.NUMBER().getText()), value);
        } else if (index instanceof TacticParser.OtherIndexContext otherIndex) {
          coeffiecients.put(
              otherIndex.elements.stream()
                  .map(Token::getText)
                  .map(Integer::parseInt)
                  .collect(Collectors.toUnmodifiableList()),
              value);
        }
      }
      final var start = context.getStart();
      return Optional.of(
          new Annotation(
              rankCoefficients,
              coeffiecients,
              "fixed at position " + start.getLine() + ":" + start.getCharPositionInLine()));
    }
    throw new IllegalArgumentException("cannot convert context");
  }

  private KnownCoefficient convert(TacticParser.NumberContext context) {
    if (context instanceof TacticParser.NatContext) {
      return new KnownCoefficient(new Fraction(Integer.parseInt(context.getText())));
    }
    if (context instanceof TacticParser.RatContext ratContext) {
      return new KnownCoefficient(
          new Fraction(
              Integer.parseInt(ratContext.numerator.getText()),
              Integer.parseInt(ratContext.denominator.getText())));
    }
    throw new IllegalArgumentException("cannot convert context");
  }
}
