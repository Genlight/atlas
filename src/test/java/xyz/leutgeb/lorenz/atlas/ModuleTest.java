package xyz.leutgeb.lorenz.atlas;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static xyz.leutgeb.lorenz.atlas.TestUtil.TACTICS;
import static xyz.leutgeb.lorenz.atlas.typing.resources.Annotation.unitIndex;
import static xyz.leutgeb.lorenz.atlas.typing.resources.coefficients.KnownCoefficient.ONE;
import static xyz.leutgeb.lorenz.atlas.typing.resources.coefficients.KnownCoefficient.ONE_BY_TWO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import xyz.leutgeb.lorenz.atlas.typing.resources.Annotation;

@Disabled
public class ModuleTest {
  static final Annotation Qp = new Annotation(List.of(ONE_BY_TWO), Map.of(unitIndex(1), ONE), "Q'");

  private static Stream<Arguments> modulesWithoutTactics() {
    return Stream.of(
        Arguments.of(
            Set.of(
                "PairingHeap.insert_isolated", "PairingHeap.delete_min_via_merge_pairs_isolated"),
            false),
        Arguments.of(Set.of("SplayHeap.insert", "SplayHeap.del_min"), false),
        Arguments.of(Set.of("SplayTree.insert", "SplayTree.delete"), false));
  }

  private static Stream<Arguments> modulesWithTactics() {
    return Stream.of(
        Arguments.of(
            Set.of(
                "PairingHeap.insert_isolated", "PairingHeap.delete_min_via_merge_pairs_isolated"),
            true),
        Arguments.of(Set.of("SplayHeap.insert", "SplayHeap.del_min"), true),
        Arguments.of(Set.of("SplayTree.insert", "SplayTree.delete"), true));
  }

  @ParameterizedTest
  @MethodSource({"modulesWithoutTactics"})
  public void test(Set<String> fqns, boolean useTactics) {
    final var program = TestUtil.loadAndNormalizeAndInferAndUnshare(fqns);
    final var result =
        program.solve(
            new HashMap<>(),
            useTactics ? program.lookupTactics(emptyMap(), TACTICS) : emptyMap(),
            true,
            true,
            false,
            emptySet());
    assertTrue(result.isSatisfiable());
    program.printAllInferredSignaturesInOrder(System.out);
  }
}
