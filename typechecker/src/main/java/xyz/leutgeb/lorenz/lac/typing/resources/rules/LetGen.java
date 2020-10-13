package xyz.leutgeb.lorenz.lac.typing.resources.rules;

import static com.google.common.collect.Sets.intersection;
import static java.util.Collections.singleton;
import static java.util.function.Predicate.not;
import static xyz.leutgeb.lorenz.lac.util.Util.bug;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import xyz.leutgeb.lorenz.lac.ast.LetExpression;
import xyz.leutgeb.lorenz.lac.typing.resources.AnnotatingContext;
import xyz.leutgeb.lorenz.lac.typing.resources.AnnotatingGlobals;
import xyz.leutgeb.lorenz.lac.typing.resources.constraints.Constraint;
import xyz.leutgeb.lorenz.lac.typing.resources.constraints.EqualityConstraint;
import xyz.leutgeb.lorenz.lac.typing.resources.proving.Obligation;
import xyz.leutgeb.lorenz.lac.typing.simple.types.TreeType;
import xyz.leutgeb.lorenz.lac.util.Pair;

public class LetGen implements Rule {
  public static final LetGen INSTANCE = new LetGen();

  public Rule.ApplicationResult apply(Obligation obligation, AnnotatingGlobals globals) {
    final var expression = (LetExpression) obligation.getExpression();
    final var declared = expression.getDeclared();
    final var value = expression.getValue();
    final var gammaDeltaQ = obligation.getContext();
    final var body = expression.getBody();
    final var x = declared.getName();
    final var qp = obligation.getAnnotation();
    final List<Constraint> crossConstraints = new ArrayList<>();

    // Γ is used as context for e1, so from the combined context,
    // take Γ to be exactly the variables that occur in e1.
    final var varsForGamma = value.freeVariables();

    final var bodyFreeVarsAsStrings = body.freeVariables();

    // Δ on the other hand is "everything that's not in Γ".
    final var varsForDelta =
        gammaDeltaQ.getIds().stream()
            .filter(not(varsForGamma::contains))
            .collect(Collectors.toSet());

    // Now, one sanity check: There must not be free variables in the body which
    // are not in Δ.
    if (!Sets.difference(Sets.difference(bodyFreeVarsAsStrings, singleton(x)), varsForDelta)
        .isEmpty()) {
      throw bug("there are free variables in the body of a let binding which do not occur in Δ");
    }

    if (!intersection(varsForGamma, varsForDelta).isEmpty()) {
      throw bug("shared variables in let expression when attempting to generate constraints");
    }

    final var gamma = new ArrayList<>(varsForGamma);
    final var delta = new ArrayList<>(varsForDelta);

    final var isTree = value.getType() instanceof TreeType;

    if (isTree) {
      throw bug("cannot apply (let:gen) if variable is not a tree");
    }

    // NOTE: Delta and x actually is delta here, because x is not a tree.
    final var deltax = new ArrayList<>(delta);

    // final var gammaP = globals.getHeuristic().generateContext("let" + x + "ΓP", gamma);
    final var gammaP = new AnnotatingContext(gamma, "P(" + x + ")");
    // final var deltaxr = globals.getHeuristic().generateContext("let" + x + "ΔR", deltax);
    final var deltaxr = new AnnotatingContext(deltax, "R(" + x + ")");

    // This is the "standard" obligation that we have to fulfill. It talks about e2
    // which is the body of the let-expression.
    //  Δ, x : α | R ⊢ e_2 : β | Q'
    final Pair<Obligation, List<Constraint>> r =
        Pair.of(obligation.keepCost(deltaxr, body, qp), new ArrayList<>());

    // Γ | P ⊢ e1 : α | ∅
    final var e1pp = globals.getHeuristic().generate("P'(" + x + ")", value);
    final Pair<Obligation, List<Constraint>> p =
        Pair.of(obligation.keepCost(gammaP, value, e1pp), new ArrayList<>());

    // For the next two constraints, note that x is neither included in Γ nor Δ.

    // Ensures that the potential through rank coefficients we get to evaluate this.value is the
    // same as we have available for this. Note that Γ ∩ Δ = ∅.
    p.getRight()
        .addAll(
            EqualityConstraint.eqRanksDefineFromLeft(
                gamma, gammaDeltaQ, gammaP, "(let:gen) q_i = p_i"));

    // Ensures that the potential through rank coefficients we get to evaluate this.body is the same
    // as we have available for this. Note that Γ ∩ Δ = ∅.
    r.getRight()
        .addAll(
            EqualityConstraint.eqRanksDefineFromLeft(
                delta, gammaDeltaQ, deltaxr, "(let:gen) q_{m + j} = r_j"));

    // Ensures that we transfer potential for Γ under Q to P (which
    // covers Γ exclusively).
    p.getRight()
        .addAll(
            gammaDeltaQ
                .streamNonRank()
                .filter(entry -> varsForDelta.isEmpty() || entry.zeroAndNonEmptyOn(varsForDelta))
                .map(
                    qEntry ->
                        new EqualityConstraint(
                            qEntry.getValue(),
                            gammaP.getCoefficientOrDefine(qEntry),
                            "(let:gen) p_{(a⃗⃗,c)} = q_{(a⃗⃗,0⃗,c)}"))
                .collect(Collectors.toSet()));

    r.getRight()
        .addAll(
            gammaDeltaQ
                .streamNonRank()
                .filter(entry -> varsForGamma.isEmpty() || entry.zeroAndNonEmptyOn(varsForGamma))
                .filter(entry -> varsForDelta.isEmpty() || entry.nonZeroOrEmptyOn(varsForDelta))
                .map(
                    qEntry ->
                        new EqualityConstraint(
                            deltaxr.getCoefficientOrDefine(qEntry),
                            qEntry.getValue(),
                            "(let:gen) ∀ b⃗ ≠ 0⃗ . r_{(b⃗,c)} = q_{(0⃗,b⃗,c)}"))
                .collect(Collectors.toUnmodifiableList()));

    return new Rule.ApplicationResult(
        List.of(p.getLeft(), r.getLeft()), List.of(p.getRight(), r.getRight()), crossConstraints);
  }

  @Override
  public String getName() {
    return "let:gen";
  }
}
