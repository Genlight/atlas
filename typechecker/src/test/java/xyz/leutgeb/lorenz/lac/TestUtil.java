package xyz.leutgeb.lorenz.lac;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import xyz.leutgeb.lorenz.lac.typing.resources.AnnotatingContext;
import xyz.leutgeb.lorenz.lac.typing.resources.Annotation;
import xyz.leutgeb.lorenz.lac.typing.resources.proving.Obligation;
import xyz.leutgeb.lorenz.lac.typing.resources.proving.Prover;

public class TestUtil {
  public static Obligation fromProver(Prover prover, Predicate<Obligation> predicate) {
    return StreamSupport.stream(prover.getProof().spliterator(), false)
        .filter(predicate)
        .findFirst()
        .orElseGet(() -> fail("missing obligation"));
  }

  public static String printTable(List<AnnotatingContext> ac) {
    return printTable(ac, emptyList());
  }

  public static String printTable(List<AnnotatingContext> ac, List<Annotation> a) {
    var ranks =
        Stream.concat(ac.stream().map(AnnotatingContext::size), a.stream().map(Annotation::size))
            .mapToInt(x -> x)
            .max()
            .orElse(0);

    var potentialFunctions =
        Stream.concat(ac.stream().map(AnnotatingContext::getAnnotation), a.stream())
            .flatMap(Annotation::streamCoefficients)
            .map(Map.Entry::getKey)
            .sorted(Annotation.INDEX_COMPARATOR)
            .distinct()
            .collect(toUnmodifiableList());

    Stream<Column<?>> columns =
        Stream.concat(
            Stream.of(
                StringColumn.create(
                    "Index",
                    Streams.concat(
                            IntStream.range(0, ranks).mapToObj(String::valueOf),
                            potentialFunctions.stream())
                        .map(Object::toString)
                        .collect(Collectors.toList()))),
            Streams.zip(
                Stream.concat(a.stream(), ac.stream().map(AnnotatingContext::getAnnotation)),
                Stream.concat(
                    a.stream().map(Annotation::getName),
                    ac.stream().map(AnnotatingContext::toString)),
                (annotation, name) ->
                    StringColumn.create(
                        name,
                        Stream.concat(
                            IntStream.range(0, ranks)
                                .mapToObj(
                                    rank ->
                                        annotation.size() > rank
                                            ? annotation.getRankCoefficient(rank).toString()
                                            : "0"),
                            potentialFunctions.stream()
                                .map(
                                    potentialFunction ->
                                        annotation.size() + 1 == potentialFunction.size()
                                            ? annotation
                                                .getCoefficientOrZero(potentialFunction)
                                                .toString()
                                            : "·")))));

    return Table.create("Overview", columns).print(Integer.MAX_VALUE / 2);
  }
}
