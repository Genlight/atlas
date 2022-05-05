package xyz.leutgeb.lorenz.atlas.typing.resources.heuristics;

import static com.google.common.collect.Lists.cartesianProduct;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.concat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import xyz.leutgeb.lorenz.atlas.typing.resources.Annotation;

@Data
public class SmartRangeHeuristic implements AnnotationHeuristic {
  public static final Set<Integer> A_RANGE = Set.of(0, 1);
  public static final Set<Integer> B_RANGE = Set.of(0, 1, 2);
  public static final Set<Integer> B_RANGE_INCL_NEGATIVE_ONE = Set.of(-1, 0, 1, 2);

  public static final SmartRangeHeuristic DEFAULT = new SmartRangeHeuristic(A_RANGE, B_RANGE);
  public static final SmartRangeHeuristic INCL_NEGATIVE_ONE =
      new SmartRangeHeuristic(A_RANGE, B_RANGE_INCL_NEGATIVE_ONE);

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
        size, range(0, size).boxed().toList(), generateInternal(size).toList(), namePrefix);
  }

  @Override
  public Annotation generate(String namePrefix, Annotation shape) {
    int size = shape.size();
    return new Annotation(
        size,
        range(0, size).boxed().toList(),
        Annotation.indexUnion(
                generateInternal(size).collect(Collectors.toList()),
                shape
                    .streamNonRankCoefficients()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList()))
            .collect(Collectors.toList()),
        namePrefix);
  }

  public Stream<List<Integer>> generateInternal(int treeSize) {
    return cartesianProduct(
            concat(Stream.generate(() -> as).limit(treeSize), Stream.of(bs)).toList())
        .stream()
        .filter(
            index -> {
              boolean hasTree = false;
              final Integer last = index.get(index.size() - 1);
              int sum = last;
              if (index.size() > 1) {
                for (int i = 0; i < index.size() - 1; i++) {
                  sum += index.get(i);
                  if (index.get(i) > 0) {
                    hasTree = true;
                  }
                }
              }
              /*
              if (last == 2) {
                return !hasTree;
              }
               */
              return sum > 0;
            });
  }
}
