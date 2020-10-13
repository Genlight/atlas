package xyz.leutgeb.lorenz.lac.ast;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import xyz.leutgeb.lorenz.lac.ast.sources.Derived;
import xyz.leutgeb.lorenz.lac.ast.sources.Predefined;
import xyz.leutgeb.lorenz.lac.ast.sources.Source;
import xyz.leutgeb.lorenz.lac.typing.simple.TypeError;
import xyz.leutgeb.lorenz.lac.typing.simple.types.BoolType;
import xyz.leutgeb.lorenz.lac.typing.simple.types.TreeType;
import xyz.leutgeb.lorenz.lac.typing.simple.types.Type;
import xyz.leutgeb.lorenz.lac.unification.UnificationContext;
import xyz.leutgeb.lorenz.lac.unification.UnificationError;
import xyz.leutgeb.lorenz.lac.util.IntIdGenerator;
import xyz.leutgeb.lorenz.lac.util.Util;

public class Identifier extends Expression {
  public static final Identifier LEAF = new Identifier(Predefined.INSTANCE, "leaf");
  private static final Set<String> CONSTANT_NAMES = Set.of("true", "false", "leaf");
  private static final Set<String> BOOLEAN_NAMES = Set.of("true", "false");

  @NonNull @Getter private final String name;
  @Getter private Intro intro;

  private Identifier(Source source, @NonNull String name) {
    super(source);
    Objects.requireNonNull(name);
    this.name = name;
  }

  public Identifier(Source source, @NonNull String name, Type type, Intro intro) {
    super(source, type);
    Objects.requireNonNull(name);
    this.name = name;
    this.intro = intro;
  }

  public static Identifier getSugar(Source source, IntIdGenerator idGenerator) {
    return get("z" + /*Util.generateSubscript*/ (idGenerator.next()), source);
  }

  public static Identifier get(String name, Source source) {
    return new Identifier(source, name);
  }

  private static boolean isConstant(String name) {
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
  public Type inferInternal(UnificationContext context) throws UnificationError, TypeError {
    if (name.equals(LEAF.name)) {
      return new TreeType(context.fresh());
    }
    if (BOOLEAN_NAMES.contains(name)) {
      return BoolType.INSTANCE;
    }

    if (context.hasSignature(this.name)) {
      return context.getSignatures().get(this.name).getType();
    }
    this.intro = context.getIntro(this.name);
    return context.getType(this.name);
  }

  @Override
  public Expression normalize(Stack<Normalization> context, IntIdGenerator idGenerator) {
    return this;
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

    return name.equals(that.name) && Objects.equals(intro, that.intro);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public Set<Identifier> freeVariables() {
    if (!(type instanceof TreeType) || isConstant()) {
      return Collections.emptySet();
    }
    return Collections.singleton(this);
  }

  private boolean isConstant() {
    return isConstant(name);
  }

  @Override
  public boolean isImmediate() {
    return !isConstant();
  }

  @Override
  public boolean isTerminal() {
    return true;
  }

  @Override
  public Identifier rename(Map<String, String> renaming) {
    if (renaming.containsValue(name)) {
      throw new IllegalArgumentException("renaming something to pre-existing name");
    }
    return new Identifier(Derived.rename(this), renaming.getOrDefault(name, name), type, intro);
  }

  @Override
  public void printHaskellTo(PrintStream out, int indentation, String currentFunction) {
    if (name.equals((LEAF.name))) {
      out.print("Leaf");
    } else if (BOOLEAN_NAMES.contains(name)) {
      out.print(Util.capitalizeFirstLetter(name));
    } else {
      out.print(name);
    }
  }

  @Override
  public Expression unshare(IntIdGenerator idGenerator, boolean lazy) {
    return this;
  }
}
