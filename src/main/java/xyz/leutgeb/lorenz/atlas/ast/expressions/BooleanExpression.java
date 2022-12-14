package xyz.leutgeb.lorenz.atlas.ast.expressions;

import static xyz.leutgeb.lorenz.atlas.ast.ComparisonOperator.EQ;
import static xyz.leutgeb.lorenz.atlas.ast.ComparisonOperator.NE;

import java.io.PrintStream;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import xyz.leutgeb.lorenz.atlas.ast.ComparisonOperator;
import xyz.leutgeb.lorenz.atlas.ast.Normalization;
import xyz.leutgeb.lorenz.atlas.ast.sources.Derived;
import xyz.leutgeb.lorenz.atlas.ast.sources.Source;
import xyz.leutgeb.lorenz.atlas.typing.simple.TypeClass;
import xyz.leutgeb.lorenz.atlas.typing.simple.TypeConstraint;
import xyz.leutgeb.lorenz.atlas.typing.simple.TypeError;
import xyz.leutgeb.lorenz.atlas.typing.simple.types.BoolType;
import xyz.leutgeb.lorenz.atlas.typing.simple.types.Type;
import xyz.leutgeb.lorenz.atlas.unification.UnificationContext;
import xyz.leutgeb.lorenz.atlas.util.IntIdGenerator;

@Value
@EqualsAndHashCode(callSuper = true)
public class BooleanExpression extends Expression {

  @NonNull Expression left;
  @NonNull ComparisonOperator operator;
  @NonNull Expression right;

  public BooleanExpression(
      Source source,
      @NonNull Expression left,
      @NonNull ComparisonOperator operator,
      @NonNull Expression right) {
    super(source);
    this.left = left;
    this.operator = operator;
    this.right = right;
  }

  private BooleanExpression(
      Source source, Expression left, ComparisonOperator operator, Expression right, Type type) {
    super(source);
    this.left = left;
    this.operator = operator;
    this.right = right;
    this.type = type;
  }

  @Override
  public Stream<? extends Expression> getChildren() {
    return Stream.of(left, right);
  }

  @Override
  public Type inferInternal(UnificationContext context) throws TypeError {
    // The next two lines hide polymorphism in favor of the "abstract base signature".
    // context.add(right.infer(context), BaseType.INSTANCE);
    // context.add(left.infer(context), BaseType.INSTANCE);

    var ty = context.fresh();
    context.addEquivalenceIfNotEqual(right.infer(context), ty, source);
    context.addEquivalenceIfNotEqual(left.infer(context), ty, source);

    final TypeClass tc =
        (EQ.equals(operator) || NE.equals(operator)) ? TypeClass.EQ : TypeClass.ORD;
    context
        .getSignature(context.getFunctionInScope(), source)
        .addConstraint(new TypeConstraint(tc, ty));
    return BoolType.INSTANCE;
  }

  @Override
  public Expression normalize(Stack<Normalization> context, IntIdGenerator idGenerator) {
    // TODO(lorenzleutgeb): Only create new expression if necessary!
    return new BooleanExpression(
        Derived.anf(this),
        left.normalize(context, idGenerator),
        operator,
        right.normalize(context, idGenerator));
  }

  @Override
  public void printTo(PrintStream out, int indentation) {
    left.printTo(out, indentation);
    out.print(" ");
    operator.printTo(out);
    out.print(" ");
    right.printTo(out, indentation);
  }

  @Override
  public void printHaskellTo(PrintStream out, int indentation, String currentFunction) {
    left.printHaskellTo(out, indentation, currentFunction);
    out.print(" ");
    operator.printHaskellTo(out);
    out.print(" ");
    right.printHaskellTo(out, indentation, currentFunction);
  }

  @Override
  public void printJavaTo(PrintStream out, int indentation, String currentFunction) {
    // Special comparison with leaf.

    if (operator.equals(EQ)) {
      if (IdentifierExpression.isLeaf(left) && IdentifierExpression.isLeaf(right)) {
        out.print("true");
        return;
      }
      if (IdentifierExpression.isLeaf(left) && !IdentifierExpression.isLeaf(right)) {
        out.print(((IdentifierExpression) right).getName() + ".isLeaf()");
        return;
      }
      if (!IdentifierExpression.isLeaf(left) && IdentifierExpression.isLeaf(right)) {
        out.print(((IdentifierExpression) left).getName() + ".isLeaf()");
        return;
      }
    }

    operator.printJavaTo(
        ((IdentifierExpression) left).getName(), ((IdentifierExpression) right).getName(), out);
  }

  @Override
  public Expression unshare(IntIdGenerator idGenerator, boolean lazy) {
    return this;
  }

  @Override
  public boolean isImmediate() {
    return false;
  }

  @Override
  public Expression rename(Map<String, String> renaming) {
    if (freeVariables().stream()
        .map(IdentifierExpression::getName)
        .anyMatch(renaming::containsKey)) {
      return new BooleanExpression(
          Derived.rename(this), left.rename(renaming), operator, right.rename(renaming), type);
    }
    return this;
  }

  @Override
  public String toString() {
    return left + " " + operator + " " + right;
  }

  @Override
  public boolean isTerminal() {
    return true;
  }
}
