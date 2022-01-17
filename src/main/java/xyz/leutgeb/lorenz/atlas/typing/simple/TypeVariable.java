package xyz.leutgeb.lorenz.atlas.typing.simple;

import static java.util.Collections.singleton;

import jakarta.json.Json;
import jakarta.json.JsonString;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import xyz.leutgeb.lorenz.atlas.typing.simple.types.Type;
import xyz.leutgeb.lorenz.atlas.unification.Equivalence;
import xyz.leutgeb.lorenz.atlas.unification.Generalizer;
import xyz.leutgeb.lorenz.atlas.unification.Substitution;
import xyz.leutgeb.lorenz.atlas.unification.TypeMismatch;
import xyz.leutgeb.lorenz.atlas.unification.UnificationContext;
import xyz.leutgeb.lorenz.atlas.unification.UnificationVariable;

// NOTE: Do not use @Value here, since have other classes inherit from TypeVariable.
public class TypeVariable implements Type {
  // private static final TypeVariable ALPHA = new TypeVariable("α");
  // private static final TypeVariable BETA = new TypeVariable("β");
  private static final TypeVariable GAMMA = new TypeVariable("γ");
  private static final TypeVariable DELTA = new TypeVariable("δ");
  private static final TypeVariable EPSILON = new TypeVariable("ε");
  private static final String[] GREEK = new String[] {"α", "β", "γ", "δ", "ε"};
  private final String name;

  public TypeVariable(String name) {
    this.name = name;
  }

  public TypeVariable(int index) {
    if (index < GREEK.length) {
      name = GREEK[index];
    } else {
      name = "ty" + index;
    }
  }

  protected String getName() {
    return name;
  }

  public static TypeVariable alpha() {
    return new TypeVariable("α");
  }

  public static TypeVariable beta() {
    return new TypeVariable("β");
  }

  @Override
  public UnificationVariable wiggle(Substitution wiggled, UnificationContext problem) {
    if (wiggled.isInDomain(this)) {
      return (UnificationVariable) wiggled.apply(this);
    }

    var result = problem.fresh();
    wiggled.substitute(this, result);
    return result;
  }

  @Override
  public Set<TypeVariable> variables() {
    return singleton(this);
  }

  @Override
  public String toHaskell() {
    return name;
  }

  @Override
  public String toJava() {
    return name;
  }

  @Override
  public JsonString toJson() {
    return Json.createValue(name);
  }

  @Override
  public Optional<Integer> countTrees() {
    return Optional.of(0);
  }

  @Override
  public Collection<Equivalence> decompose(Type b) throws TypeMismatch {
    throw new TypeMismatch(this, b);
  }

  @Override
  public boolean occurs(TypeVariable b) {
    return equals(b);
  }

  @Override
  public Type substitute(TypeVariable v, Type t) {
    return v.equals(this) ? t : this;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public TypeVariable generalize(Generalizer generalizer) {
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TypeVariable that = (TypeVariable) o;

    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
