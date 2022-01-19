package xyz.leutgeb.lorenz.atlas.ast.expressions;

import static java.util.Collections.singleton;

import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import xyz.leutgeb.lorenz.atlas.ast.Intro;
import xyz.leutgeb.lorenz.atlas.ast.Normalization;
import xyz.leutgeb.lorenz.atlas.ast.SystemIntro;
import xyz.leutgeb.lorenz.atlas.ast.sources.Derived;
import xyz.leutgeb.lorenz.atlas.ast.sources.Predefined;
import xyz.leutgeb.lorenz.atlas.ast.sources.Source;
import xyz.leutgeb.lorenz.atlas.typing.simple.TypeError;
import xyz.leutgeb.lorenz.atlas.typing.simple.TypeVariable;
import xyz.leutgeb.lorenz.atlas.typing.simple.types.BoolType;
import xyz.leutgeb.lorenz.atlas.typing.simple.types.TreeType;
import xyz.leutgeb.lorenz.atlas.typing.simple.types.Type;
import xyz.leutgeb.lorenz.atlas.unification.UnificationContext;
import xyz.leutgeb.lorenz.atlas.util.IntIdGenerator;
import xyz.leutgeb.lorenz.atlas.util.Util;

public class IdentifierExpression extends Expression {
  private static final String LEAF_NAME = "leaf";
  private static final String COIN_NAME = "coin";
  private static final IdentifierExpression UNDERSCORE =
      new IdentifierExpression(Predefined.INSTANCE, "_");
  private static final Set<String> BOOLEAN_NAMES = Set.of("true", "false");
  private static final Set<String> SPECIAL_NAMES =
      Sets.union(BOOLEAN_NAMES, Set.of(LEAF_NAME, COIN_NAME));

  @NonNull @Getter private final String name;
  @Getter private Intro intro;

  private IdentifierExpression(Source source, @NonNull String name) {
    super(source);
    Objects.requireNonNull(name);
    this.name = name;
  }

  public IdentifierExpression(Source source, @NonNull String name, Type type, Intro intro) {
    super(source, type);
    Objects.requireNonNull(name);
    this.name = name;
    this.intro = intro;
  }

  private static IdentifierExpression predefined(String name, Type type) {
    return new IdentifierExpression(Predefined.INSTANCE, name, type, SystemIntro.INSTANCE);
  }

  public static IdentifierExpression predefinedBase(String name, TypeVariable type) {
    return new IdentifierExpression(Predefined.INSTANCE, name, type, SystemIntro.INSTANCE);
  }

  public static IdentifierExpression predefinedBase(String name) {
    return predefinedBase(name, TypeVariable.alpha());
  }

  public static IdentifierExpression predefinedTree(String name) {
    return predefinedTree(name, TypeVariable.alpha());
  }

  public static IdentifierExpression predefinedTree(String name, TypeVariable typeVariable) {
    return predefined(name, new TreeType(typeVariable));
  }

  public static IdentifierExpression getSugar(Source source, IntIdGenerator idGenerator) {
    return get("z" + (idGenerator.next()), source);
  }

  public static IdentifierExpression get(String name, Source source) {
    return new IdentifierExpression(source, name);
  }

  private static boolean isSpecial(String name) {
    return SPECIAL_NAMES.contains(name);
  }

  public static Expression anonymous(Source source, IntIdGenerator idGenerator) {
    return get("_" + idGenerator.next(), source);
  }

  public static IdentifierExpression leaf() {
    return new IdentifierExpression(Predefined.INSTANCE, LEAF_NAME);
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
  public Type inferInternal(UnificationContext context) throws TypeError {
    if (name.equals(LEAF_NAME)) {
      return new TreeType(context.fresh());
    }
    if (BOOLEAN_NAMES.contains(name) || name.equals(COIN_NAME)) {
      return BoolType.INSTANCE;
    }
    if (name.startsWith("_")) {
      return context.fresh();
    }
    if (context.hasSignature(this.name)) {
      return context.getSignatures().get(this.name).getType();
    }
    this.intro = context.getIntro(this.name);
    return context.getType(this.name, source);
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

    IdentifierExpression that = (IdentifierExpression) o;

    return name.equals(that.name) && Objects.equals(intro, that.intro);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public Set<IdentifierExpression> freeVariables() {
    if (!(type instanceof TreeType) || isSpecial()) {
      return Collections.emptySet();
    }
    return singleton(this);
  }

  private boolean isSpecial() {
    return isSpecial(name);
  }

  @Override
  public boolean isImmediate() {
    return !LEAF_NAME.equals(name);
  }

  @Override
  public boolean isTerminal() {
    return true;
  }

  @Override
  public IdentifierExpression rename(Map<String, String> renaming) {
    if (renaming.containsValue(name)) {
      throw new IllegalArgumentException("renaming something to pre-existing name");
    }
    return new IdentifierExpression(
        Derived.rename(this), renaming.getOrDefault(name, name), type, intro);
  }

  @Override
  public void printHaskellTo(PrintStream out, int indentation, String currentFunction) {
    if (name.equals((LEAF_NAME))) {
      out.print("Leaf");
    } else if (BOOLEAN_NAMES.contains(name)) {
      out.print(Util.capitalizeFirstLetter(name));
    } else {
      out.print(name);
    }
  }

  @Override
  public void printJavaTo(PrintStream out, int indentation, String currentFunction) {
    if (name.equals((LEAF_NAME))) {
      out.print("Tree.<" + type.variables().iterator().next() + ">leaf()");
    } else if (name.equals((COIN_NAME))) {
      out.print("coin()");
    } else {
      out.print(name);
    }
  }

  @Override
  public Expression unshare(IntIdGenerator idGenerator, boolean lazy) {
    return this;
  }

  @Override
  public boolean isTreeConstruction() {
    return getType() instanceof TreeType;
  }

  public static boolean isLeaf(Expression identifier) {
    return identifier instanceof IdentifierExpression
        && LEAF_NAME.equals(((IdentifierExpression) identifier).getName());
  }

  public static boolean isCoin(Expression identifier) {
    return identifier instanceof IdentifierExpression
        && COIN_NAME.equals(((IdentifierExpression) identifier).getName());
  }
}
