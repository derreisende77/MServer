package de.mediathekview.mserver.ui.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.TypeConversionException;

@Command(
    name = "MServer",
    mixinStandardHelpOptions = true,
    description = "Runs MServer crawler and maintenance flows.")
public class MServerCommandLine {
  public enum CMDARG {
    config,
    gconf,
    flow,
    topicsSearchEnabled,
    invalid;
    
    public static CMDARG from(String key) {
      try {
          return CMDARG.valueOf(key);
      } catch (IllegalArgumentException e) {
          return CMDARG.invalid;
      }
    }
  }

  enum Flow {
    importFilmlistIntoDB,
    exportFilmListFromDB,
    checkAvailability;

    static Flow from(final String value) {
      for (final Flow flow : values()) {
        if (flow.name().equalsIgnoreCase(value)) {
          return flow;
        }
      }
      throw new TypeConversionException("Unknown flow: " + value);
    }
  }

  public static class FlowConverter implements ITypeConverter<Flow> {
    @Override
    public Flow convert(final String value) {
      return Flow.from(value);
    }
  }

  @Option(
      names = "--config",
      paramLabel = "FILE_OR_URL",
      description = "Configuration file path or URL.")
  private String config;

  @Option(names = "--gconf", description = "Generate the default configuration file.")
  private boolean generateDefaultConfiguration;

  @Option(
      names = "--flow",
      paramLabel = "FLOW",
      converter = FlowConverter.class,
      description =
          "Maintenance flow: importFilmlistIntoDB, exportFilmListFromDB, checkAvailability.")
  private Flow flow;

  @Option(
      names = "--topicsSearchEnabled",
      arity = "0..1",
      fallbackValue = "true",
      paramLabel = "true|false",
      description = "Override topic search setting.")
  private Boolean topicsSearchEnabled;

  static Optional<MServerCommandLine> parse(final String[] args) {
    final MServerCommandLine commandLineArguments = new MServerCommandLine();
    final CommandLine commandLine = new CommandLine(commandLineArguments);

    try {
      final ParseResult parseResult = commandLine.parseArgs(args);
      if (CommandLine.printHelpIfRequested(parseResult)) {
        return Optional.empty();
      }
      return Optional.of(commandLineArguments);
    } catch (final ParameterException parameterException) {
      commandLine.getErr().println(parameterException.getMessage());
      commandLine.usage(commandLine.getErr());
      return Optional.empty();
    }
  }

  public static void print() {
    new CommandLine(new MServerCommandLine()).usage(System.err);
  }

  static boolean validateArgs(String[] args) {
    try {
      parseArgs(args);
      return true;
    } catch (final ParameterException parameterException) {
      return false;
    }
  }

  static Map<CMDARG, String> parseArgs(String[] args) {
    final MServerCommandLine commandLineArguments = new MServerCommandLine();
    new CommandLine(commandLineArguments).parseArgs(args);
    return commandLineArguments.toLegacyMap();
  }

  Map<CMDARG, String> toLegacyMap() {
    Map<CMDARG, String> map = new HashMap<>();
    if (config != null) {
      map.put(CMDARG.config, config);
    }
    if (generateDefaultConfiguration) {
      map.put(CMDARG.gconf, "true");
    }
    if (flow != null) {
      map.put(CMDARG.flow, flow.name());
    }
    if (topicsSearchEnabled != null) {
      map.put(CMDARG.topicsSearchEnabled, topicsSearchEnabled.toString());
    }
    return map;
  }
  
}
