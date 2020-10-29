package xyz.leutgeb.lorenz.lac.commands;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import picocli.CommandLine;

@Substitute
@TargetClass(className = "xyz.leutgeb.lorenz.lac.commands.REPL")
@CommandLine.Command(name = REPL.NAME, hidden = true)
public final class REPLSubstitute {}
