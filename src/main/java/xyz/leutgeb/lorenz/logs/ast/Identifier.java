package xyz.leutgeb.lorenz.logs.ast;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.hipparchus.util.Pair;
import xyz.leutgeb.lorenz.logs.Context;
import xyz.leutgeb.lorenz.logs.type.TreeType;
import xyz.leutgeb.lorenz.logs.type.Type;
import xyz.leutgeb.lorenz.logs.type.TypeError;
import xyz.leutgeb.lorenz.logs.unification.UnificationError;

@EqualsAndHashCode(callSuper = false)
public class Identifier extends TupleElement {
  public static final Identifier NIL = new Identifier(Predefined.INSTANCE, "nil");
  public static final Identifier TRUE = new Identifier(Predefined.INSTANCE, "true");
  public static final Identifier FALSE = new Identifier(Predefined.INSTANCE, "false");

  private static final Interner<Identifier> INTERNER = Interners.newWeakInterner();

  private static int freshness = 0;

  @NonNull @Getter private final String name;
  @NonNull @Getter private final Set<Source> occurences;

  static {
    INTERNER.intern(NIL);
    INTERNER.intern(TRUE);
    INTERNER.intern(FALSE);
  }

  public Identifier(Source source, @NonNull String name) {
    super(source);
    Objects.requireNonNull(name);
    this.name = name;
    this.occurences = new HashSet<>();
    this.occurences.add(source);
  }

  public static Identifier get() {
    return get("_" + freshness++);
  }

  public static Identifier getSugar() {
    return get("∂" + generateSubscript(freshness++));
  }

  private static String generateSubscript(int i) {
    StringBuilder sb = new StringBuilder();
    for (char ch : String.valueOf(i).toCharArray()) {
      sb.append((char) ('\u2080' + (ch - '0')));
    }
    return sb.toString();
  }

  public static Identifier get(String name) {
    return INTERNER.intern(new Identifier(Predefined.INSTANCE, name));
  }

  public static Identifier get(String name, Source source) {
    Identifier identifier = INTERNER.intern(new Identifier(source, name));
    identifier.occurences.add(source);
    return identifier;
  }

  @Override
  public String toString() {
    return "(id " + name + ")";
  }

  @Override
  public Type infer(Context context) throws UnificationError, TypeError {
    if (this == NIL) {
      return new TreeType(context.getProblem().fresh());
    }

    Type ty = context.lookup(this.name);
    if (ty == null) {
      throw new TypeError.NotInContext(this.name);
    } else {
      return ty;
    }
  }

  @Override
  public Expression normalize(Stack<Pair<Identifier, Expression>> context) {
    return this;
  }

  public boolean isImmediate() {
    return true;
  }
}
