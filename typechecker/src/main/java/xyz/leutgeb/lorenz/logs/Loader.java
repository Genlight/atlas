package xyz.leutgeb.lorenz.logs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Value;
import org.antlr.v4.runtime.CharStreams;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.IntegerComponentNameProvider;
import org.jgrapht.traverse.TopologicalOrderIterator;
import xyz.leutgeb.lorenz.logs.ast.FunctionDefinition;
import xyz.leutgeb.lorenz.logs.ast.Program;

@Value
public class Loader {
  private static final String DOT_EXTENSION = ".ml";
  @Getter private final Map<String, FunctionDefinition> functionDefinitions = new HashMap<>();
  Path home;

  // Edges in this graph point from the dependency to the dependant. This way,
  // it can be topologically traversed for simple type inference.
  private final DirectedAcyclicGraph<String, DefaultEdge> g =
      new DirectedAcyclicGraph<>(DefaultEdge.class);

  public Loader(Path home) {
    if (!Files.exists(home) || !Files.isDirectory(home) || !Files.isReadable(home)) {
      throw new IllegalArgumentException("home must be an existing readable directory");
    }
    if (!home.isAbsolute()) {
      home = home.toAbsolutePath();
    }
    this.home = home;
  }

  public static Loader atCurrentWorkingDirectory() {
    return new Loader(currentWorkingDirectory());
  }

  private static Path currentWorkingDirectory() {
    return Path.of(".").toAbsolutePath();
  }

  public static Path path(String moduleName, Path home, String extension) {
    // Actual path to module on disk.
    var modulePath = moduleName.split("\\.");
    modulePath[modulePath.length - 1] += extension;

    var path = home;
    for (int i = 0; i < modulePath.length; i++) {
      path = path.resolve(modulePath[i]);
    }
    return path;
  }

  public static Path path(String moduleName, Path home) {
    return path(moduleName, home, DOT_EXTENSION);
  }

  public static String moduleName(String fqn) {
    var lastDot = fqn.lastIndexOf(".");

    // Logical module name.
    return fqn.substring(0, lastDot);
  }

  public String moduleName(Path path) {
    if (!path.getFileName().toString().endsWith(DOT_EXTENSION)) {
      throw new IllegalArgumentException();
    }
    final var sb = new StringBuilder();
    final var relative = home.relativize(path);
    final var len = relative.getNameCount();
    for (int i = 0; i < len - 1; i++) {
      sb.append(relative.getName(i).getFileName());
    }
    final var last = relative.getName(len - 1).toString();
    sb.append(last, 0, last.length() - DOT_EXTENSION.length());
    return sb.toString();
  }

  public Program all() throws IOException {
    return load(functionDefinitions.keySet());
  }

  public void autoload() throws IOException {
    final var stack = new Stack<String>();
    Files.find(
            home,
            256,
            ((path, basicFileAttributes) ->
                path.getFileName().toString().endsWith(DOT_EXTENSION) && looksGood(path)))
        .peek(System.out::println)
        .flatMap(
            path -> {
              try {
                return ModuleParser.parse(CharStreams.fromPath(path), moduleName(path)).stream();
              } catch (IOException e) {
                e.printStackTrace();
                return Stream.empty();
              }
            })
        .forEach(
            fd -> {
              functionDefinitions.putIfAbsent(fd.getFullyQualifiedName(), fd);
              stack.push(fd.getFullyQualifiedName());
            });
    load(stack);
  }

  public Program loadInline(String source) throws IOException {
    final var definitions = ModuleParser.parse(source, "_");
    if (definitions.size() > 1) {
      throw new IllegalArgumentException("exactly one function definition is required");
    }
    final var definition = definitions.get(0);
    functionDefinitions.put(definition.getFullyQualifiedName(), definition);
    return load(definitions.get(0).getOcurringFunctions());
  }

  public Program load(String name) throws IOException {
    return load(Collections.singleton(name));
  }

  public Program loadMatching(Pattern pattern) throws IOException {
    return load(
        functionDefinitions.keySet().stream()
            .filter(pattern.asMatchPredicate())
            .collect(Collectors.toSet()));
  }

  public Program load(Set<String> names) throws IOException {
    final var stack =
        names.stream()
            .filter(Predicate.not(functionDefinitions::containsKey))
            .collect(Collectors.toCollection(Stack::new));
    load(stack);
    final var connectivityInspector = new ConnectivityInspector<>(g);

    final var iterator =
        new TopologicalOrderIterator<>(
            new AsSubgraph<>(
                g,
                names.stream()
                    .flatMap(name -> connectivityInspector.connectedSetOf(name).stream())
                    .collect(Collectors.toSet())));

    final var result = new ArrayList<FunctionDefinition>();
    while (iterator.hasNext()) {
      final var fqn = iterator.next();
      final var fd = functionDefinitions.get(fqn);
      if (fd == null) {
        throw new RuntimeException("no definition for '" + fqn + "'");
      }
      result.add(fd);
    }
    return new Program(result);
  }

  public Path path(String moduleName) {
    return path(moduleName, home);
  }

  private void load(Stack<String> stack) throws IOException {
    while (!stack.isEmpty()) {
      final var fqn = stack.pop();

      // Logical module name.
      final var moduleName = moduleName(fqn);

      // Actual path to module on disk.
      final var path = path(moduleName);

      if (!looksGood(path)) {
        throw new RuntimeException("could not resolve path for function name '" + fqn + "'");
      }

      if (functionDefinitions.containsKey(fqn)) {
        ingest(functionDefinitions.get(fqn), stack);
      } else {
        // Ingest all definitions that were parsed, no matter whether we actually "need"
        // them. This implementation might be a bit too eager, since it will load
        // all function definitions in a file even though they might not be dependencies.
        // This could be improved.
        var definitions = ModuleParser.parse(CharStreams.fromPath(path), moduleName);
        for (var definition : definitions) {
          ingest(definition, stack);
        }
      }
    }
  }

  private void ingest(FunctionDefinition definition, Stack<String> stack) {
    functionDefinitions.putIfAbsent(definition.getFullyQualifiedName(), definition);
    if (!g.containsVertex(definition.getFullyQualifiedName())) {
      g.addVertex(definition.getFullyQualifiedName());
    }
    for (var dependency : definition.getOcurringFunctionsNonRecursive()) {
      if (!g.containsVertex(dependency)) {
        g.addVertex(dependency);
        stack.push(dependency);
      }
      g.addEdge(dependency, definition.getFullyQualifiedName());
    }
  }

  private boolean looksGood(Path path) {
    return Files.exists(path) && Files.isReadable(path) && Files.isRegularFile(path);
  }

  public void exportGraph(OutputStream stream) throws ExportException {
    final var exporter =
        new DOTExporter<>(
            new IntegerComponentNameProvider<>(), (String vertex) -> vertex, (DefaultEdge e) -> "");
    exporter.exportGraph(g, stream);
  }
}
