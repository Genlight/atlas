package xyz.leutgeb.lorenz.lac.typing.simple.types;

import java.util.Collection;
import java.util.Collections;
import xyz.leutgeb.lorenz.lac.typing.simple.TypeVariable;
import xyz.leutgeb.lorenz.lac.unification.Equivalence;
import xyz.leutgeb.lorenz.lac.unification.Generalizer;
import xyz.leutgeb.lorenz.lac.unification.Substitution;
import xyz.leutgeb.lorenz.lac.unification.TypeMismatch;
import xyz.leutgeb.lorenz.lac.unification.UnificationContext;

public class BoolType implements Type {
  public static final BoolType INSTANCE = new BoolType();

  @Override
  public Collection<Equivalence> decompose(Type b) throws TypeMismatch {
    if (!(b instanceof BoolType)) {
      throw new TypeMismatch(this, b);
    }
    return Collections.emptyList();
  }

  @Override
  public boolean occurs(TypeVariable b) {
    return false;
  }

  @Override
  public Type substitute(TypeVariable v, Type t) {
    return this;
  }

  @Override
  public Type generalize(Generalizer g) {
    return this;
  }

  @Override
  public Type wiggle(Substitution wiggled, UnificationContext context) {
    return this;
  }

  @Override
  public String toHaskell() {
    return "Bool";
  }

  @Override
  public String toString() {
    return "Bool";
  }
}
