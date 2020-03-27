package xyz.leutgeb.lorenz.lac.ast;

import static com.google.common.collect.Sets.intersection;
import static xyz.leutgeb.lorenz.lac.Util.indent;
import static xyz.leutgeb.lorenz.lac.Util.pick;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Stream;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hipparchus.util.Pair;
import xyz.leutgeb.lorenz.lac.IntIdGenerator;
import xyz.leutgeb.lorenz.lac.ast.sources.Derived;
import xyz.leutgeb.lorenz.lac.ast.sources.Source;
import xyz.leutgeb.lorenz.lac.typing.simple.TypeError;
import xyz.leutgeb.lorenz.lac.typing.simple.types.Type;
import xyz.leutgeb.lorenz.lac.unification.UnificationContext;
import xyz.leutgeb.lorenz.lac.unification.UnificationError;

@Data
@EqualsAndHashCode(callSuper = true)
public class LetExpression extends Expression {
  private final Identifier declared;
  private final Expression value;

  private final Expression body;

  public LetExpression(Source source, Identifier declared, Expression value, Expression body) {
    super(source);
    this.declared = declared;
    this.value = value;
    this.body = body;
  }

  private LetExpression(
      Source source, Identifier declared, Expression value, Expression body, Type type) {
    super(source, type);
    this.declared = declared;
    this.value = value;
    this.body = body;
  }

  @Override
  public Stream<? extends Expression> getChildren() {
    return Stream.concat(follow(), Stream.of(declared));
  }

  @Override
  public Stream<? extends Expression> follow() {
    return Stream.of(value, body);
  }

  @Override
  public Type inferInternal(UnificationContext context) throws UnificationError, TypeError {
    var declaredType = context.getProblem().fresh();
    context.getProblem().addIfNotEqual(this, declaredType, value.infer(context).wiggle(context));
    var sub = context.child();
    sub.putType(declared.getName(), declaredType);
    sub.getProblem().addIfNotEqual(this, declaredType, declared.infer(sub).wiggle(context));

    var result = context.getProblem().fresh();
    sub.getProblem().addIfNotEqual(this, result, body.infer(sub).wiggle(context));
    return result;
  }

  @Override
  public Expression normalize(
      Stack<Pair<Identifier, Expression>> context, IntIdGenerator idGenerator) {
    Stack<Pair<Identifier, Expression>> sub = new Stack<>();
    return new LetExpression(
            source, declared, value.normalize(sub, idGenerator), body.normalizeAndBind(idGenerator))
        .bindAll(sub);
  }

  @Override
  public Expression rename(Map<String, String> renaming) {
    return new LetExpression(
        Derived.rename(this), declared, value.rename(renaming), body.rename(renaming), type);
  }

  @Override
  public void printTo(PrintStream out, int indentation) {
    out.print("let ");
    declared.printTo(out, indentation);
    out.print(" = ");
    value.printTo(out, indentation + 1);
    // out.println();
    // indent(out, indentation);
    out.println(" in (");
    indent(out, indentation);
    body.printTo(out, indentation + 1);
    out.println();
    indent(out, indentation - 1);
    out.print(")");
  }

  @Override
  public void printHaskellTo(PrintStream out, int indentation) {
    out.print("let ");
    declared.printHaskellTo(out, indentation);
    out.print(" = ");
    value.printHaskellTo(out, indentation + 1);
    // out.println();
    // indent(out, indentation);
    out.println(" in (");
    indent(out, indentation);
    body.printHaskellTo(out, indentation + 1);
    out.println();
    indent(out, indentation);
    out.print(")");
  }

  @Override
  public Set<Identifier> freeVariables() {
    final var result = super.freeVariables();
    result.remove(declared);
    return result;
  }

  @Override
  public String toString() {
    return "let " + declared + " = " + value.terminalOrBox() + " in " + body.terminalOrBox();
  }

  @Override
  public Expression unshare(Map<String, Integer> unshared, IntIdGenerator idGenerator) {
    final var newValue = value.unshare(unshared, idGenerator);
    final var newBody = body.unshare(unshared, idGenerator);

    final var intersection = intersection(newValue.freeVariables(), newBody.freeVariables());

    if (intersection.isEmpty()) {
      return new LetExpression(source, declared, newValue, newBody, type);
    }

    // Otherwise, there's some overlap between body and value.
    var target = pick(intersection);
    var down = ShareExpression.clone(target, unshared, idGenerator);
    var result = ShareExpression.rename(target, down, Pair.create(newValue, newBody));

    Expression newThis =
        new LetExpression(source, declared, result.getFirst(), result.getSecond(), type);

    if (intersection.size() > 1) {
      newThis = newThis.unshare(unshared, idGenerator);
    }

    return new ShareExpression(this, target, down, newThis);
  }
}