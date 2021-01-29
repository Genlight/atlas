package xyz.leutgeb.lorenz.lac.typing.resources.heuristics;

import static com.google.common.collect.Lists.cartesianProduct;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.concat;
import static xyz.leutgeb.lorenz.lac.typing.resources.Annotation.isConstantIndex;
import static xyz.leutgeb.lorenz.lac.typing.resources.Annotation.isUnitIndex;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import xyz.leutgeb.lorenz.lac.typing.resources.Annotation;
import xyz.leutgeb.lorenz.lac.util.Util;

@Data
public class SmartRangeHeuristic implements AnnotationHeuristic {
  public static final SmartRangeHeuristic DEFAULT =
      new SmartRangeHeuristic(Set.of(0, 1), Set.of(0, 1, 2));

  private final List<Integer> as;
  private final List<Integer> bs;
  private final boolean nonZero;

  public SmartRangeHeuristic(Set<Integer> as, Set<Integer> bs, boolean nonZero) {
    this.as = List.copyOf(as);
    this.bs = List.copyOf(bs);
    this.nonZero = nonZero;
  }

  public SmartRangeHeuristic(Set<Integer> as, Set<Integer> bs) {
    this(as, bs, true);
  }

  @Override
  public Annotation generate(String namePrefix, int size) {
    return new Annotation(
        size,
        range(0, size).boxed().collect(toUnmodifiableList()),
        generate(size).collect(Collectors.toUnmodifiableList()),
        namePrefix);
  }

  public Stream<List<Integer>> generate(int treeSize) {
    return cartesianProduct(
            concat(Stream.generate(() -> as).limit(treeSize), Stream.of(bs))
                .collect(toUnmodifiableList()))
        .stream()
        .filter(Predicate.not(Util::isAllZeroes))
        // TODO: Be smart about when to allow constants, especially 1. Only when there's a leaf?
        .filter(index -> !isConstantIndex(index) || isUnitIndex(index));
  }
}
