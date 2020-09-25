package xyz.leutgeb.lorenz.lac.typing.resources.constraints;

import static java.util.stream.Collectors.toSet;

import com.google.common.collect.BiMap;
import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Node;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import xyz.leutgeb.lorenz.lac.typing.resources.coefficients.Coefficient;
import xyz.leutgeb.lorenz.lac.typing.resources.coefficients.UnknownCoefficient;

@Value
@Slf4j
public class DisjunctiveConstraint extends Constraint {
  List<Constraint> elements;

  public DisjunctiveConstraint(List<Constraint> elements, String reason) {
    super(reason);
    this.elements = elements;
  }

  @Override
  public BoolExpr encode(Context ctx, BiMap<UnknownCoefficient, ArithExpr> coefficients) {
    return ctx.mkOr(
        elements.stream()
            .map(element -> element.encode(ctx, coefficients))
            .toArray(BoolExpr[]::new));
  }

  @Override
  public Graph toGraph(Graph graph, Map<Coefficient, Node> nodes) {
    log.warn("cannot convert disjunctive constraint to graph");
    return graph;
  }

  @Override
  public Constraint replace(Coefficient target, Coefficient replacement) {
    return new DisjunctiveConstraint(
        elements.stream()
            .map(element -> element.replace(target, replacement))
            .collect(Collectors.toList()),
        getReason());
  }

  @Override
  public Set<Coefficient> occurringCoefficients() {
    return elements.stream()
        .map(Constraint::occurringCoefficients)
        .flatMap(Set::stream)
        .collect(toSet());
  }

  @Override
  public String toString() {
    if (elements.isEmpty()) {
      return "false";
    }
    return elements.stream().map(Object::toString).collect(Collectors.joining(" ∨ ", "(", ")"));
  }

  @Override
  public Stream<Constraint> children() {
    return elements.stream();
  }
}
