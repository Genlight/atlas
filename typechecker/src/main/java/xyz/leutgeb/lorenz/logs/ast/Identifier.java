package xyz.leutgeb.lorenz.logs.ast;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import org.hipparchus.util.Pair;
import xyz.leutgeb.lorenz.logs.Context;
import xyz.leutgeb.lorenz.logs.Util;
import xyz.leutgeb.lorenz.logs.ast.sources.Predefined;
import xyz.leutgeb.lorenz.logs.ast.sources.Renamed;
import xyz.leutgeb.lorenz.logs.ast.sources.Source;
import xyz.leutgeb.lorenz.logs.resources.AnnotatingContext;
import xyz.leutgeb.lorenz.logs.resources.AnnotatingGlobals;
import xyz.leutgeb.lorenz.logs.resources.Annotation;
import xyz.leutgeb.lorenz.logs.resources.coefficients.Coefficient;
import xyz.leutgeb.lorenz.logs.typing.TypeError;
import xyz.leutgeb.lorenz.logs.typing.types.BoolType;
import xyz.leutgeb.lorenz.logs.typing.types.TreeType;
import xyz.leutgeb.lorenz.logs.typing.types.Type;
import xyz.leutgeb.lorenz.logs.unification.UnificationError;

public class Identifier extends Expression {
  public static final Identifier NIL = new Identifier(Predefined.INSTANCE, "nil");
  private static final Set<String> CONSTANT_NAMES = Set.of("true", "false", "nil");
  private static final Set<String> BOOLEAN_NAMES = Set.of("true", "false");
  private static int freshness = 0;
  @NonNull @Getter private final String name;
  @NonNull @Getter private final Set<Source> occurences;

  public Identifier(Source source, @NonNull String name) {
    super(source);
    Objects.requireNonNull(name);
    this.name = name;
    this.occurences = new HashSet<>();
    this.occurences.add(source);
  }

  public Identifier(Source source, @NonNull String name, Type type) {
    super(source, type);
    Objects.requireNonNull(name);
    this.name = name;
    this.occurences = new HashSet<>();
    this.occurences.add(source);
  }

  public static final Identifier nil() {
    return new Identifier(Predefined.INSTANCE, "nil");
  }

  public static Identifier getSugar(Source source) {
    // TODO: Instead of a global counter, have one per function definition.
    return get("var" + freshness++, source);
    // return get("∂" + Util.generateSubscript(freshness++));
  }

  private static Identifier get(String name) {
    return new Identifier(Predefined.INSTANCE, name);
  }

  public static Identifier get(String name, Source source) {
    Identifier identifier = new Identifier(source, name);
    identifier.occurences.add(source);
    return identifier;
  }

  public static boolean isConstant(String name) {
    return CONSTANT_NAMES.contains(name);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public Stream<? extends Expression> getChildren() {
    return Stream.empty();
  }

  @Override
  public Type inferInternal(Context context) throws UnificationError, TypeError {
    if (name.equals(NIL.name)) {
      return new TreeType(context.getProblem().fresh());
    }
    if (BOOLEAN_NAMES.contains(name)) {
      return BoolType.INSTANCE;
    }

    if (context.hasSignature(this.name)) {
      return context.getSignatures().get(this.name).getType();
    }
    return context.lookupType(this.name);
  }

  @Override
  public Expression normalize(Stack<Pair<Identifier, Expression>> context) {
    return this;
  }

  @Override
  public Annotation inferAnnotationsInternal(AnnotatingContext context, AnnotatingGlobals globals)
      throws UnificationError, TypeError {
    // Three cases
    //  - nil
    //    see rule (nil)
    //  - tree variables
    //    return some annotation
    //  - non-tree variables or constants (like true/false)
    //    return a zero-valued annotation
    final var constraints = globals.getConstraints();
    if (NIL.name.equals(name)) {
      var qp = constraints.heuristic(1);

      // Apply (w : var) and (w) since this is a leaf.
      var q =
          context.weakenAllIdentifiers(this, constraints).getAnnotation().less(this, constraints);

      for (var e : q.getCoefficients()) {
        final int c = e.getKey().get(0);
        final var value = e.getValue();
        final var sum = new ArrayList<Coefficient>();
        for (final var d : qp.getCoefficients()) {
          final var index = d.getKey();
          final var a = index.get(0);
          final var b = index.get(1);
          if (a + b == c) {
            sum.add(d.getValue());
          }
        }
        constraints.eqSum(this, value, sum);
      }
      return qp.greater(this, constraints);
    } else if (getType() instanceof TreeType) {
      return context
          .weakenIdentifiersExcept(this, constraints, this)
          .getAnnotation()
          .greater(this, globals.getConstraints());
    } else {
      return Annotation.empty().greater(this, constraints);
    }
  }

  @Override
  public void printTo(PrintStream out, int indentation) {
    out.print(name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Identifier that = (Identifier) o;

    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public Set<String> freeVariables() {
    if (!(type instanceof TreeType) || isConstant()) {
      return Collections.emptySet();
    }
    return Collections.singleton(name);
  }

  public boolean isConstant() {
    return isConstant(name);
  }

  @Override
  public boolean isImmediate() {
    return !isConstant();
  }

  @Override
  public Identifier rename(Map<String, String> renaming) {
    return new Identifier(new Renamed(source), renaming.getOrDefault(name, name), type);
  }

  @Override
  public void printHaskellTo(PrintStream out, int indentation) {
    if (name.equals((NIL.name))) {
      out.print("Nil");
    } else if (BOOLEAN_NAMES.contains(name)) {
      out.print(Util.capitalizeFirstLetter(name));
    } else {
      out.print(name);
    }
  }
}
