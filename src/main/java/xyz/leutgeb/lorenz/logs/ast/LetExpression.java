package xyz.leutgeb.lorenz.logs.ast;

import java.util.Stack;
import lombok.Data;
import org.hipparchus.util.Pair;
import xyz.leutgeb.lorenz.logs.Context;
import xyz.leutgeb.lorenz.logs.type.Type;
import xyz.leutgeb.lorenz.logs.type.TypeError;
import xyz.leutgeb.lorenz.logs.unification.UnificationError;

@Data
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

  @Override
  public Type infer(Context context) throws UnificationError, TypeError {
    var declaredType = context.getProblem().fresh();
    context.getProblem().add(declaredType, value.infer(context));
    var sub = new Context(context);
    sub.put(declared.getName(), declaredType);
    sub.getProblem().add(declaredType, declared.infer(sub));

    var result = context.getProblem().fresh();
    sub.getProblem().add(result, body.infer(sub));
    return result;
  }

  @Override
  public Expression normalize(Stack<Pair<Identifier, Expression>> context) {
    Stack<Pair<Identifier, Expression>> sub = new Stack<>();
    return new LetExpression(source, declared, value.normalize(sub), body).bindAll(sub);
  }
}
