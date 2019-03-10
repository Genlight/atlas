package xyz.leutgeb.lorenz.logs.visitor;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.Token;
import xyz.leutgeb.lorenz.logs.SymbolTable;
import xyz.leutgeb.lorenz.logs.antlr.SplayBaseVisitor;
import xyz.leutgeb.lorenz.logs.antlr.SplayParser;
import xyz.leutgeb.lorenz.logs.ast.FunctionDefinition;
import xyz.leutgeb.lorenz.logs.ast.Identifier;

@RequiredArgsConstructor
public class FunctionDefinitionVisitor extends SplayBaseVisitor<FunctionDefinition> {
  private final SymbolTable symbolTable;

  @Override
  public FunctionDefinition visitFunc(SplayParser.FuncContext ctx) {
    final SymbolTable symbolTable = new SymbolTable(this.symbolTable);

    List<String> args = new ArrayList<>(ctx.args.size());
    for (Token t : ctx.args) {
      String arg = t.getText();
      args.add(arg);
      SymbolTable.Entry entry = new SymbolTable.Entry(null, t);
      Identifier expr = Identifier.get(arg);
      symbolTable.put(expr, entry);
    }
    ExpressionVisitor visitor = new ExpressionVisitor(symbolTable);
    return new FunctionDefinition(ctx.name.getText(), args, visitor.visit(ctx.body));
  }
}
