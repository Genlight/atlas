package xyz.leutgeb.lorenz.atlas.unification;

import static com.google.common.collect.Sets.difference;

import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import xyz.leutgeb.lorenz.atlas.ast.FunctionDefinition;
import xyz.leutgeb.lorenz.atlas.ast.Program;
import xyz.leutgeb.lorenz.atlas.ast.SourceIntro;
import xyz.leutgeb.lorenz.atlas.ast.expressions.Expression;
import xyz.leutgeb.lorenz.atlas.ast.expressions.LetExpression;
import xyz.leutgeb.lorenz.atlas.ast.sources.Source;
import xyz.leutgeb.lorenz.atlas.typing.simple.FunctionSignature;
import xyz.leutgeb.lorenz.atlas.typing.simple.TypeError;
import xyz.leutgeb.lorenz.atlas.typing.simple.types.FunctionType;
import xyz.leutgeb.lorenz.atlas.typing.simple.types.Type;
import xyz.leutgeb.lorenz.atlas.util.IntIdGenerator;
import xyz.leutgeb.lorenz.atlas.util.Util;

@Slf4j
@Value
public class UnificationContext {
  UnificationContext parent;

  /**
   * Holds types for identifiers. This is extended through {@link #putType(String, Type)}, for
   * example by walking over {@link LetExpression}.
   */
  Map<String, Type> types;

  Map<String, SourceIntro> intros;

  Set<String> hidden;

  Program program;

  String functionInScope;

  LinkedList<Equivalence> equivalences;

  IntIdGenerator intIdGenerator;

  private UnificationContext(
      UnificationContext parent,
      Map<String, Type> types,
      Map<String, SourceIntro> intros,
      Set<String> hidden,
      Program program,
      String functionInScope,
      LinkedList<Equivalence> equivalences,
      IntIdGenerator intIdGenerator) {
    this.parent = parent;
    this.types = types;
    this.intros = intros;
    this.hidden = hidden;
    this.program = program;
    this.functionInScope = functionInScope;
    this.equivalences = equivalences;
    this.intIdGenerator = intIdGenerator;
  }

  public static UnificationContext root(Program program) {
    return new UnificationContext(
        null,
        new LinkedHashMap<>(),
        new HashMap<>(),
        new HashSet<>(),
        program,
        null,
        new LinkedList<>(),
        IntIdGenerator.fromZeroInclusive());
  }

  public UnificationContext child() {
    return new UnificationContext(
        this,
        new LinkedHashMap<>(),
        new HashMap<>(),
        new HashSet<>(),
        this.program,
        this.functionInScope,
        this.equivalences,
        this.intIdGenerator);
  }

  public UnificationContext childWithNewVariables(String fqn) {
    return new UnificationContext(
        null,
        new LinkedHashMap<>(),
        new HashMap<>(),
        new HashSet<>(),
        this.program,
        fqn,
        this.equivalences,
        this.intIdGenerator);
  }

  public UnificationContext childWithNewProblem() {
    return new UnificationContext(
        null,
        new LinkedHashMap<>(),
        new HashMap<>(),
        new HashSet<>(),
        this.program,
        null,
        new LinkedList<>(),
        IntIdGenerator.fromZeroInclusive());
  }

  public String toString() {
    return "["
        + this.equivalences.stream()
            .map(Object::toString)
            .collect(Collectors.joining(", ", "{", "}"))
        + " {"
        + this.types.entrySet().stream()
            .map(e -> e.getKey() + " :: " + e.getValue())
            .collect(Collectors.joining(", "))
        + "}]";
  }

  /** Recursively looks up the signature of some identifier (given as {@link String}). */
  public @Nonnull Type getType(final String id, Source source) throws TypeError {
    if (isHiddenRecursive(id)) {
      throw new TypeError("'" + id + "' is out of scope.", source);
    }
    Type t = getTypeRecursive(id);
    if (t != null) {
      return t;
    }
    throw new TypeError(Util.undefinedText(id, iterateIdentifiers()), source);
  }

  public SourceIntro getIntro(final String id) {
    return getIntroRecursive(id);
  }

  public @Nonnull FunctionSignature getSignature(final String fqn, Source source) throws TypeError {
    FunctionSignature t = program.getFunctionDefinitions().get(fqn).getInferredSignature();
    if (t != null) {
      return t;
    }

    throw new TypeError(Util.undefinedText(fqn, iterateIdentifiers()), source);
  }

  public Optional<FunctionSignature> maybeGetSignature(final String fqn) {
    return Optional.ofNullable(program.getFunctionDefinitions().get(fqn))
        .map(FunctionDefinition::getInferredSignature);
  }

  private Type getTypeInternal(final String key) {
    return isHiddenRecursive(key) ? null : getTypeRecursive(key);
  }

  private boolean isHiddenRecursive(final String key) {
    return hidden.contains(key) || (parent != null && parent.isHiddenRecursive(key));
  }

  private Type getTypeRecursive(final String key) {
    Type t = types.get(key);
    if (t != null) {
      return t;
    } else if (parent != null) {
      return parent.getTypeRecursive(key);
    } else {
      return null;
    }
  }

  private SourceIntro getIntroRecursive(final String key) {
    SourceIntro t = intros.get(key);
    if (t != null) {
      return t;
    } else if (parent != null) {
      return parent.getIntroRecursive(key);
    } else {
      return null;
    }
  }

  private boolean hasType(String key) {
    if (hidden.contains(key)) {
      return false;
    }
    Type t = types.get(key);
    if (t != null) {
      return true;
    } else if (parent != null) {
      return parent.hasType(key);
    } else {
      return false;
    }
  }

  public UnificationContext hide(String... variables) {
    final var result = child();
    result.hidden.addAll(Arrays.asList(variables));
    return result;
  }

  public void putType(String key, Type value, Expression intro) {
    if (value instanceof FunctionType) {
      throw new IllegalArgumentException("use putSignature");
    }

    if ("leaf".equals(key)) { // || "_".equals(key)) {
      // Silently ignore this, since leaf and _ will have a magic type assigned in
      // IdentifierExpression.
      return;
    }

    if (getTypeInternal(key) != null) {
      log.debug("Hiding variable '{}' at {}", key, intro.getSource().getRoot());
      // throw new RuntimeException("hiding of variables not possible (affected: '" + key + "')");
    }

    types.put(key, value);
    intros.put(key, new SourceIntro(this.functionInScope, intro));
  }

  private Iterator<String> iterateIdentifiers() {
    final var names = difference(types.keySet(), hidden);
    final var it = names.iterator();
    return parent == null
        ? it
        : Iterators.concat(
            it, Iterators.filter(parent.iterateIdentifiers(), name -> !hidden.contains(name)));
  }

  public void addEquivalenceIfNotEqual(Type a, Type b, Source source) {
    Objects.requireNonNull(a);
    Objects.requireNonNull(b);
    if (!a.equals(b)) {
      equivalences.add(new Equivalence(a, b, source));
    }
  }

  public UnificationVariable fresh() {
    return new UnificationVariable(intIdGenerator.next());
  }
}
