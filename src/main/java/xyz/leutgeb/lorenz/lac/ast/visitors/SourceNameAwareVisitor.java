package xyz.leutgeb.lorenz.lac.ast.visitors;

import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.ParserRuleContext;
import xyz.leutgeb.lorenz.lac.antlr.SplayBaseVisitor;
import xyz.leutgeb.lorenz.lac.ast.sources.Parsed;
import xyz.leutgeb.lorenz.lac.ast.sources.Source;

@RequiredArgsConstructor
public class SourceNameAwareVisitor<T> extends SplayBaseVisitor<T> {
  @Getter private final String moduleName;
  @Getter private final Path path;

  Source getSource(ParserRuleContext context) {
    return new Parsed(context, path);
  }
}
