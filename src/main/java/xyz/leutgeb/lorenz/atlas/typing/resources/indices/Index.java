package xyz.leutgeb.lorenz.atlas.typing.resources.indices;

import static xyz.leutgeb.lorenz.atlas.typing.resources.Annotation.INDEX_COMPARATOR;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.Value;
import xyz.leutgeb.lorenz.atlas.ast.expressions.IdentifierExpression;
import xyz.leutgeb.lorenz.atlas.util.Util;

public interface Index {
  Integer getAssociatedIndex(IdentifierExpression id);

  Integer getOffsetIndex();

  Index mask(Map<IdentifierExpression, Integer> maskMap);

  Index mask(Integer maskedOffsetIndex);

  Index mask(Function<IdentifierExpression, Integer> maskFunction);

  Index addToOffset(int x);

  default List<Integer> instantiate(Collection<IdentifierExpression> ids) {
    return ids.stream().map(this::getAssociatedIndex).toList();
  }

  default List<Integer> instantiateWithOffset(Collection<IdentifierExpression> ids) {
    return Stream.concat(ids.stream().map(this::getAssociatedIndex), Stream.of(getOffsetIndex()))
        .toList();
  }

  default List<Integer> instantiateWithOffset(Collection<IdentifierExpression> ids, int offset) {
    return Stream.concat(ids.stream().map(this::getAssociatedIndex), Stream.of(offset)).toList();
  }

  default Index mask(IdentifierExpression id, Integer associatedIndex) {
    return mask(Map.of(id, associatedIndex));
  }

  default Index padWithZero() {
    return new FunctionIndex(Util.fallback(this::getAssociatedIndex, (id) -> 0), getOffsetIndex());
  }

  default boolean agreeOnAssociatedIndices(Index other, Set<IdentifierExpression> identifiers) {
    // NOTE: If there are no associated indices for given identifiers, then this evaluates to true!
    return identifiers.stream()
        .allMatch(id -> getAssociatedIndex(id).equals(other.getAssociatedIndex(id)));
  }

  default boolean allAssociatedIndicesMatch(
      Collection<IdentifierExpression> ids, Predicate<Integer> predicate) {
    return ids.stream().map(this::getAssociatedIndex).allMatch(predicate);
  }

  @Deprecated
  default boolean nonZeroOrEmptyOn(Collection<IdentifierExpression> ids) {
    if (ids.isEmpty()) {
      return true;
    }
    return ids.stream().anyMatch(id -> getAssociatedIndex(id) != 0);
  }

  @Deprecated
  default boolean zeroAndNonEmptyOn(Collection<IdentifierExpression> ids) {
    if (ids.isEmpty()) {
      return false;
    }
    return ids.stream().allMatch(id -> getAssociatedIndex(id) == 0);
  }

  @Value
  class DomainComparator implements Comparator<Index> {
    List<IdentifierExpression> domain;

    @Override
    public int compare(Index o1, Index o2) {
      return INDEX_COMPARATOR.compare(manifest(o1)::iterator, manifest(o2)::iterator);
    }

    private Stream<Integer> manifest(Index index) {
      final var paddedIndex = index.padWithZero();
      return Stream.concat(
          domain.stream().map(paddedIndex::getAssociatedIndex), Stream.of(index.getOffsetIndex()));
    }
  }
}
