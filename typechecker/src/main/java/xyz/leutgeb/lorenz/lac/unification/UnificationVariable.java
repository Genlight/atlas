package xyz.leutgeb.lorenz.lac.unification;

import java.util.Collection;
import java.util.Collections;
import lombok.EqualsAndHashCode;
import lombok.Value;
import xyz.leutgeb.lorenz.lac.typing.simple.TypeVariable;
import xyz.leutgeb.lorenz.lac.typing.simple.types.Type;

@Value
@EqualsAndHashCode(callSuper = true)
public class UnificationVariable extends TypeVariable {
  public UnificationVariable(int id) {
    super(id);
  }

  public TypeVariable generalize(Generalizer g) {
    return g.generalize(this);
  }

  @Override
  public Collection<Equivalence> decompose(Type b) throws TypeMismatch {
    return Collections.emptyList();
  }

  public Type substitute(TypeVariable v, Type t) {
    return v.equals(this) ? t : this;
  }

  @Override
  public UnificationVariable wiggle(Substitution wiggled, UnificationProblem context) {
    return this;
  }

  @Override
  public String toString() {
    return "?" + getName();
  }
}
