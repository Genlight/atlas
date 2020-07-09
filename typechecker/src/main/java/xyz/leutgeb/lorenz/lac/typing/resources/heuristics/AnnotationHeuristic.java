package xyz.leutgeb.lorenz.lac.typing.resources.heuristics;

import java.util.Collection;
import java.util.List;
import xyz.leutgeb.lorenz.lac.Util;
import xyz.leutgeb.lorenz.lac.ast.Expression;
import xyz.leutgeb.lorenz.lac.typing.resources.AnnotatingContext;
import xyz.leutgeb.lorenz.lac.typing.resources.Annotation;
import xyz.leutgeb.lorenz.lac.typing.simple.types.TreeType;

public interface AnnotationHeuristic {
  Annotation generate(String namePrefix, int size);

  default Annotation generate(String namePrefix, Annotation annotation) {
    return generate(namePrefix, annotation.size());
  }

  default Annotation generate(String namePrefix, Collection<?> collection) {
    return generate(namePrefix, collection.size());
  }

  default AnnotatingContext generateContext(String namePrefix, List<String> ids) {
    return new AnnotatingContext(ids, generate(namePrefix, ids));
  }

  default Annotation generate(Expression expression) {
    return generate("_" + Util.randomHex(), expression.getType() instanceof TreeType ? 1 : 0);
  }
}
