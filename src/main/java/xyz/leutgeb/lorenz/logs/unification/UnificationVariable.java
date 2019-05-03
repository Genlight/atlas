package xyz.leutgeb.lorenz.logs.unification;

import java.util.Collection;
import java.util.Collections;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.leutgeb.lorenz.logs.Util;
import xyz.leutgeb.lorenz.logs.type.Type;
import xyz.leutgeb.lorenz.logs.type.TypeVariable;

@Data
@EqualsAndHashCode(callSuper = true)
public class UnificationVariable extends TypeVariable {
  public UnificationVariable(int id) {
    super("?" + Util.generateSubscript(id));
  }

  public Type generalize(Generalizer g) {
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
  public String toString() {
    return super.toString();
  }
}
