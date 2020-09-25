package xyz.leutgeb.lorenz.lac.typing.resources.coefficients;

import static xyz.leutgeb.lorenz.lac.util.Util.bug;
import static xyz.leutgeb.lorenz.lac.util.Util.randomHex;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UnknownCoefficient implements Coefficient {
  String name;
  boolean negated;
  boolean maybeNegative;

  public UnknownCoefficient(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name cannot be null or blank");
    }
    this.name = name;
    this.negated = false;
    this.maybeNegative = false;
  }

  private UnknownCoefficient(String name, boolean negated) {
    this.name = name;
    this.negated = negated;
    this.maybeNegative = false;
  }

  public static UnknownCoefficient maybeNegative(String name) {
    return new UnknownCoefficient(name, false, true);
  }

  public static UnknownCoefficient unknown(String namePrefix) {
    return new UnknownCoefficient(namePrefix + randomHex(4));
  }

  @Override
  public UnknownCoefficient negate() {
    return new UnknownCoefficient(name, !negated);
  }

  @Override
  public String toString() {
    return (negated ? "-" : "") + name;
  }

  public ArithExpr encode(Context ctx, Map<UnknownCoefficient, ArithExpr> coefficients) {
    final var inner = coefficients.get(this.canonical());
    return negated ? ctx.mkUnaryMinus(inner) : inner;
  }

  @Override
  public Coefficient replace(Coefficient target, Coefficient replacement) {
    if (negated) {
      throw bug("don't know how to replace negated coefficient");
    }
    if (target instanceof UnknownCoefficient unknownCoefficient) {
      if (unknownCoefficient.name.equals(name)) {
        return replacement;
      }
    }
    return this;
  }

  @Override
  public Coefficient canonical() {
    return negated ? negate() : this;
  }
}
