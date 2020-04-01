package xyz.leutgeb.lorenz.lac.unification;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Value;
import xyz.leutgeb.lorenz.lac.ast.Expression;
import xyz.leutgeb.lorenz.lac.typing.simple.types.Type;

@Value
public class Equivalence {
  Type left;
  Type right;

  /** Optionally, keep the expression that justifies this equivalence. */
  @Nullable Expression justification;

  public Equivalence(Type left, Type right, @Nullable Expression justification) {
    requireNonNull(left);
    requireNonNull(right);

    if (left.equals(right)) {
      throw new IllegalArgumentException(
          "two types that are equal should not be explicitly said to be equivalent");
    }

    if (!(left instanceof UnificationVariable) && (right instanceof UnificationVariable)) {
      this.left = right;
      this.right = left;
    } else {
      this.left = left;
      this.right = right;
    }
    this.justification = justification;
  }

  /** Constructs an instance without justification. Use with care. */
  public Equivalence(Type left, Type right) {
    this(left, right, null);
  }

  public Optional<Equivalence> substitute(UnificationVariable variable, Type result) {
    var leftSubstitute = left.substitute(variable, result);
    var rightSubstitute = right.substitute(variable, result);
    if (!leftSubstitute.equals(rightSubstitute)) {
      return Optional.of(new Equivalence(leftSubstitute, rightSubstitute, justification));
    }
    return Optional.empty();
  }

  public void occurs() throws OccursError {
    if (left instanceof UnificationVariable && right.occurs((UnificationVariable) left)) {
      throw new OccursError(left, right);
    }
  }

  public Collection<Equivalence> unify() throws UnificationError {
    if (left == right) {
      return emptyList();
    }
    return left.decompose(right);
  }

  @Override
  public String toString() {
    return left + " ≈ " + right;
  }
}
