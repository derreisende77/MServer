package de.mediathekview.mserver.ui.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.mediathekview.mserver.ui.config.MServerCommandLine.CMDARG;
import java.util.Map;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.ParameterException;

class MServerCommandLineTest {

  @Test
  void parseArgsAcceptsEmptyInvocation() {
    final Map<CMDARG, String> args = MServerCommandLine.parseArgs(new String[0]);

    assertThat(args).isEmpty();
  }

  @Test
  void parseArgsRejectsMissingConfigValue() {
    assertThatThrownBy(
            () ->
                MServerCommandLine.parseArgs(
                    new String[] {"--config", "--flow", "checkAvailability"}))
        .isInstanceOf(ParameterException.class);
  }

  @Test
  void parseArgsRejectsUnknownFlowValue() {
    assertThatThrownBy(
            () -> MServerCommandLine.parseArgs(new String[] {"--flow", "checkAvailabilty"}))
        .isInstanceOf(ParameterException.class)
        .hasMessageContaining("Unknown flow");
  }

  @Test
  void parseArgsMapsSupportedOptionsToLegacyMap() {
    final Map<CMDARG, String> args =
        MServerCommandLine.parseArgs(
            new String[] {
              "--config",
              "MServer-Config.yaml",
              "--gconf",
              "--flow",
              "checkAvailability",
              "--topicsSearchEnabled",
              "false"
            });

    assertThat(args)
        .containsEntry(CMDARG.config, "MServer-Config.yaml")
        .containsEntry(CMDARG.gconf, "true")
        .containsEntry(CMDARG.flow, "checkAvailability")
        .containsEntry(CMDARG.topicsSearchEnabled, "false");
  }

  @Test
  void parseArgsSupportsTopicsSearchEnabledAsFlag() {
    final Map<CMDARG, String> args =
        MServerCommandLine.parseArgs(new String[] {"--topicsSearchEnabled"});

    assertThat(args).containsEntry(CMDARG.topicsSearchEnabled, "true");
  }
}
