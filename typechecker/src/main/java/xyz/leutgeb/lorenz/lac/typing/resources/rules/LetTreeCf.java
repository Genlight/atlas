package xyz.leutgeb.lorenz.lac.typing.resources.rules;

import static com.google.common.collect.Sets.cartesianProduct;
import static com.google.common.collect.Sets.intersection;
import static java.util.Collections.singleton;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static xyz.leutgeb.lorenz.lac.typing.resources.coefficients.KnownCoefficient.ZERO;
import static xyz.leutgeb.lorenz.lac.util.Util.bug;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import xyz.leutgeb.lorenz.lac.ast.Identifier;
import xyz.leutgeb.lorenz.lac.ast.LetExpression;
import xyz.leutgeb.lorenz.lac.typing.resources.AnnotatingContext;
import xyz.leutgeb.lorenz.lac.typing.resources.AnnotatingGlobals;
import xyz.leutgeb.lorenz.lac.typing.resources.Annotation;
import xyz.leutgeb.lorenz.lac.typing.resources.constraints.Constraint;
import xyz.leutgeb.lorenz.lac.typing.resources.constraints.DisjunctiveConstraint;
import xyz.leutgeb.lorenz.lac.typing.resources.constraints.EqualityConstraint;
import xyz.leutgeb.lorenz.lac.typing.resources.constraints.EqualsSumConstraint;
import xyz.leutgeb.lorenz.lac.typing.resources.constraints.LessThanOrEqualConstraint;
import xyz.leutgeb.lorenz.lac.typing.resources.indices.Index;
import xyz.leutgeb.lorenz.lac.typing.resources.indices.MapIndex;
import xyz.leutgeb.lorenz.lac.typing.resources.proving.Obligation;
import xyz.leutgeb.lorenz.lac.typing.simple.types.TreeType;
import xyz.leutgeb.lorenz.lac.util.Pair;
import xyz.leutgeb.lorenz.lac.util.Util;

public class LetTreeCf implements Rule {
  public static final LetTreeCf INSTANCE = new LetTreeCf();

  private static final Set<Integer> D_RANGE = Set.of(0, 1);
  private static final Set<Integer> E_RANGE = Set.of(0, 2);
  private static final Set<List<Integer>> DE_RANGE = cartesianProduct(D_RANGE, E_RANGE);

  private static final record BDE(List<Integer> b, int d, int e) {
    public List<Integer> toIndex() {
      final var result = new ArrayList<Integer>(b.size() + 2);
      result.addAll(b);
      result.add(d);
      result.add(e);
      return result;
    }

    public static BDE fromIndex(List<Integer> index) {
      int n = index.size() - 2;
      if (index.subList(0, n).stream().allMatch(x -> x == 0)) {
        throw new IllegalArgumentException();
      }
      return new BDE(index.subList(0, n), index.get(n - 1), index.get(n));
    }
  }

  /**
   * P wird durch Q definiert: * p_i = q_i * p_(a,c) = q_(a,0,c)
   *
   * <p>P^(b,d,e) wird durch Q definiert: * sum ... p^(b,d,e)_(a,c) = q_(a,b,c) * p^(b,d,e)_i = 0
   * (implizit, keine Constraints)
   *
   * <p>P'^(b,d,e) wird durch P^(b,d,e) definiert: * p'^(b,d,e)_(d,e) ist 0 oder kleiner gleich
   * p^(b,d,e)_(a,c) * p'^(b,d,e)_i = 0 (implizit, keine Constraints)
   *
   * <p>R wird durch Q, P' und P'^(b,d,e) definiert: * r_j = q_{m+j} * r_{k+1} = p'_* * r_(b,d,e) =
   * p'^(b,d,e)_(d,e) (mit b != nullvektor) * r_(0,d,e) = p'_(d,e)
   *
   * <p>P' wird nicht direkt aus anderen Annotationen definiert, sondern es wird eine Form
   * angenommen, z.B. (a, c) mit a in {0, 1}, c in {0, 2}.
   */
  public ApplicationResult apply(Obligation obligation, AnnotatingGlobals globals) {
    final var expression = (LetExpression) obligation.getExpression();
    final var x = expression.getDeclared();
    final var value = expression.getValue();
    final var gammaDeltaQ = obligation.getContext();
    final var body = expression.getBody();
    final List<Constraint> crossConstraints = new ArrayList<>();

    if (!(value.getType() instanceof TreeType)) {
      throw bug("cannot apply (let:tree:cf) to a variable that is not a tree");
    }

    // Γ is used as context for e1, so from the combined context,
    // take Γ to be exactly the variables that occur in e1.
    final var varsForGammaAsSet = value.freeVariables();

    // Δ on the other hand is "everything that's not in Γ".
    final var varsForDeltaAsSet =
        gammaDeltaQ.getIds().stream()
            .filter(not(varsForGammaAsSet::contains))
            .collect(Collectors.toSet());

    // Now, one sanity check: There must not be free variables in the body which
    // are not in Δ.
    if (!Sets.difference(Sets.difference(body.freeVariables(), singleton(x)), varsForDeltaAsSet)
        .isEmpty()) {
      throw bug("there are free variables in the body of a let binding which do not occur in Δ");
    }

    if (!intersection(varsForGammaAsSet, varsForDeltaAsSet).isEmpty()) {
      throw bug("shared variables in let expression when attempting to generate constraints");
    }

    final var varsForGammaAsList =
        obligation.getContext().getIds().stream()
            .filter(varsForGammaAsSet::contains)
            .collect(Collectors.toUnmodifiableList());

    final var varsForDeltaAsList =
        obligation.getContext().getIds().stream()
            .filter(varsForDeltaAsSet::contains)
            .collect(Collectors.toUnmodifiableList());

    // Construct the context Γ | P
    // P will be defined by Q.
    final var gammaP = new AnnotatingContext(varsForGammaAsList, "P(" + x + ")");

    // First main obligation about the value being bound by this let-expression.
    // Γ | P  ⊢  e1 : T | P'
    final var pp = globals.getHeuristic().generate("P'(" + x + ")", value);
    final Pair<Obligation, List<Constraint>> p =
        Pair.of(obligation.keepCost(gammaP, value, pp), new ArrayList<>());

    // Construct the context Δ, x : Tree | R
    // R will be defined by P' and P'^{(\vec{b},d,e)}.
    final var deltax = new ArrayList<>(varsForDeltaAsList);
    deltax.add(x);
    final var deltaxr = new AnnotatingContext(deltax, "R(" + x + ")");

    // Second main obligation about the body of this let-expression.
    // Δ, x : Tree | R  ⊢  e2 : β | Q'
    final Pair<Obligation, List<Constraint>> r =
        Pair.of(obligation.keepAnnotationAndCost(deltaxr, body), new ArrayList<>());

    // Define rank coefficients in P from Q.
    p.getRight()
        .addAll(
            EqualityConstraint.eqRanksDefineFromLeft(
                varsForGammaAsList, gammaDeltaQ, gammaP, "(let:tree:cf) q_i = p_i"));

    // Define non-rank coefficients in P from Q.
    p.getRight()
        .addAll(
            gammaDeltaQ
                .streamNonRank()
                // Ensure that b = 0.
                .filter(
                    entry ->
                        varsForDeltaAsSet.isEmpty() || entry.zeroAndNonEmptyOn(varsForDeltaAsSet))
                // Ensure that we do not point at a = 0 and c = 0.
                .filter(
                    entry -> {
                      if (varsForGammaAsList.isEmpty()) {
                        return entry.getOffsetIndex() > 0;
                      }
                      if (entry.getOffsetIndex() == 0) {
                        return !entry.allAssociatedIndicesMatch(varsForGammaAsList, a -> a == 0);
                      }
                      return true;
                    })
                .map(
                    entry ->
                        new EqualityConstraint(
                            gammaP.getCoefficientOrDefine(entry),
                            entry.getValue(),
                            "(let:tree:cf) p_{(a⃗⃗,c)} = q_{(a⃗⃗,0⃗,c)} with [a⃗⃗, c] = "
                                + entry.toString()))
                .collect(Collectors.toSet()));

    // Define rank coefficients in R from Q.
    r.getRight()
        .addAll(
            EqualityConstraint.eqRanksDefineFromLeft(
                varsForDeltaAsList, gammaDeltaQ, deltaxr, "(let:tree:cf) q_{m+j} = r_j"));

    // Define rank coefficient for x in R from P'.
    crossConstraints.add(
        // Since the result of evaluating this.value is effectively the same as the newly
        // introduced variable, equate those coefficients.
        new EqualityConstraint(
            deltaxr.getRankCoefficientOrDefine(x),
            pp.getRankCoefficient(),
            "(let:tree:cf) r_{k+1} = p'_{*}"));

    // Define some non-rank coefficients in R from P'.
    crossConstraints.addAll(
        pp.streamNonRankCoefficients()
            .map(
                e -> {
                  final var index = e.getKey();
                  return new EqualityConstraint(
                      deltaxr.getCoefficientOrDefine(
                          id -> id.equals(x) ? index.get(0) : 0, index.get(1)),
                      e.getValue(),
                      "(let:tree:cf) r_{(0⃗,d,e)} = p'_{(d,e)}");
                })
            .collect(toList()));

    // Define some non-rank coefficients in R from Q.
    crossConstraints.addAll(
        gammaDeltaQ
            .streamNonRank()
            .filter(
                entry ->
                    // Ensure that b is not zero.
                    varsForGammaAsList.isEmpty()
                        || entry.allAssociatedIndicesMatch(varsForGammaAsList, b -> b == 0))
            .filter(
                entry ->
                    // Ensure that a is zero.
                    varsForDeltaAsList.isEmpty()
                        || !entry.allAssociatedIndicesMatch(varsForDeltaAsList, zero -> zero == 0))
            .map(
                entry ->
                    new EqualityConstraint(
                        deltaxr.getCoefficientOrDefine(entry.mask(x, 0)),
                        entry.getValue(),
                        "(let:tree:cf) r_{(b⃗,0,e)} = q_{(0⃗,b⃗,e)}"))
            .collect(toList()));

    final Map<Index, Pair<Obligation, List<Constraint>>> pbdes = new HashMap<>();

    if (!varsForDeltaAsList.isEmpty()) {
      // Find all indices (\vec{b}, d, e) such that \vec{b} \neq \vec{0}.
      final List<Index> bdes =
          gammaDeltaQ
              .streamNonRank()
              .filter(
                  entry ->
                      varsForDeltaAsList.isEmpty()
                          || !entry.allAssociatedIndicesMatch(varsForDeltaAsList, b -> b == 0))
              .flatMap(
                  entry ->
                      DE_RANGE.stream()
                          // .filter(not(Util::isZero))
                          .map(
                              de -> {
                                Map<Identifier, Integer> associatedIndices = new HashMap<>();
                                for (Identifier id : varsForDeltaAsList) {
                                  associatedIndices.put(id, entry.getAssociatedIndex(id));
                                }
                                associatedIndices.put(x, de.get(0));
                                return new MapIndex(associatedIndices, de.get(1));
                              }))
              .distinct()
              .collect(Collectors.toUnmodifiableList());

      final Function<? super Index, Pair<Obligation, List<Constraint>>> pbProducer =
          (key) ->
              Pair.of(
                  new Obligation(
                      new AnnotatingContext(varsForGammaAsList, "P(" + x + ")(" + key + ")"),
                      value,
                      new Annotation(1, "P'(" + x + ")(" + key + ")"),
                      0),
                  new ArrayList<>());

      gammaDeltaQ
          .streamNonRank()
          .filter(entry1 -> entry1.nonZeroOrEmptyOn(varsForDeltaAsList))
          .filter(
              entry -> {
                // If a is empty or zero, then c must be non-zero
                if (entry.allAssociatedIndicesMatch(varsForGammaAsList, a -> a == 0)) {
                  return entry.getOffsetIndex() > 0;
                }
                // If c is zero, then a must be non-empty and non-zero
                if (entry.getOffsetIndex() == 0) {
                  return !varsForGammaAsList.isEmpty()
                      && !entry.allAssociatedIndicesMatch(varsForGammaAsList, a -> a == 0);
                }
                return true;
              })
          .filter(
              entry ->
                  varsForGammaAsList.isEmpty()
                      || !entry.allAssociatedIndicesMatch(varsForGammaAsList, a -> a == 0))
          .map(
              qEntry ->
                  new EqualsSumConstraint(
                      qEntry.getValue(),
                      bdes.stream()
                          .filter(bde -> qEntry.agreeOnAssociatedIndices(bde, varsForDeltaAsSet))
                          .map(
                              bde ->
                                  pbdes
                                      .computeIfAbsent(bde, pbProducer)
                                      .getLeft()
                                      .getContext()
                                      .getCoefficientOrDefine(qEntry))
                          .collect(Collectors.toUnmodifiableList()),
                      "q_{(a⃗⃗,b⃗,c)} = Σ_{(d,e)}{p^{(b⃗,d,e)}_{(a⃗⃗,c})}"))
          .forEach(crossConstraints::add);

      for (var bde : bdes) {
        final var d = bde.getAssociatedIndex(x);
        final var e = bde.getOffsetIndex();

        final var cfpair = pbdes.computeIfAbsent(bde, pbProducer);
        final var cfobligation = cfpair.getLeft();
        final var cfconstraints = cfpair.getRight();

        final var cfp = cfobligation.getContext().getAnnotation();
        final var cfpp = cfobligation.getAnnotation();

        if (d != 0 || e != 0) {
          cfconstraints.add(
              new EqualityConstraint(
                  deltaxr.getCoefficientOrDefine(bde),
                  cfpp.getCoefficientOrDefine(d, e),
                  "(let:tree:cf) r_{(b⃗,d,e)} = p'^{(b⃗,d,e)}_{(d,e)}"));
        }

        DE_RANGE.stream()
            .filter(Predicate.not(Util::isZero))
            .map(Pair::of)
            .filter(dpep -> !dpep.getLeft().equals(d) || !dpep.getRight().equals(e))
            .map(
                dpep ->
                    new EqualityConstraint(
                        cfpp.getCoefficientOrDefine(dpep.getLeft(), dpep.getRight()),
                        ZERO,
                        "(let:tree:cf) p'^{(b⃗,d,e}_{(d',e')}=0"))
            .forEach(cfconstraints::add);

        if (d != 0 || e != 0) {
          // TODO: Maybe use implication instead of DisjunctiveConstraint
          cfp.streamNonRankCoefficients()
              .filter(entry -> entry.getKey().size() == 1 || entry.getKey().get(0) != 0)
              .map(
                  entry ->
                      new DisjunctiveConstraint(
                          List.of(
                              new EqualityConstraint(
                                  entry.getValue(),
                                  ZERO,
                                  "(let:tree:cf) p^{(b⃗,d,e}_{(a⃗⃗,c)} == 0"),
                              new LessThanOrEqualConstraint(
                                  cfpp.getCoefficientOrDefine(d, e),
                                  entry.getValue(),
                                  "(let:tree:cf) p'^{(b⃗,d,e}_{(d,e)} ≤ p^{(b⃗,d,e}_{(a⃗⃗,c)}")),
                          "(let:tree:cf) implication in line 3"))
              .forEach(cfconstraints::add);
        }
      }
    }

    final var old = Util.append(List.of(p, r), List.copyOf(pbdes.values()));
    final var result =
        new ApplicationResult(
            old.stream().map(Pair::getLeft).collect(Collectors.toUnmodifiableList()),
            old.stream().map(Pair::getRight).collect(Collectors.toUnmodifiableList()),
            crossConstraints);
    return result;
  }

  @Override
  public String getName() {
    return "let:tree:cf";
  }
}
