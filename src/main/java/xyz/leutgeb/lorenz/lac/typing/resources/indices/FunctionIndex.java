package xyz.leutgeb.lorenz.lac.typing.resources.indices;

import java.util.Map;
import java.util.function.Function;
import xyz.leutgeb.lorenz.lac.ast.Identifier;
import xyz.leutgeb.lorenz.lac.util.Util;

public class FunctionIndex implements Index {
  Function<Identifier, Integer> associatedIndices;
  Integer offsetIndex;

  public FunctionIndex(Function<Identifier, Integer> associatedIndices, Integer offsetIndex) {
    this.associatedIndices = associatedIndices;
    this.offsetIndex = offsetIndex;
  }

  @Override
  public Integer getAssociatedIndex(Identifier id) {
    return associatedIndices.apply(id);
  }

  @Override
  public Integer getOffsetIndex() {
    return offsetIndex;
  }

  @Override
  public Index mask(Map<Identifier, Integer> maskMap) {
    return new FunctionIndex(Util.fallback(maskMap::get, associatedIndices), offsetIndex);
  }

  @Override
  public Index mask(Integer maskedOffsetIndex) {
    return new FunctionIndex(associatedIndices, maskedOffsetIndex);
  }

  @Override
  public Index mask(Function<Identifier, Integer> maskFunction) {
    return new FunctionIndex(maskFunction, offsetIndex);
  }
}
