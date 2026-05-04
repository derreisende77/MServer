package de.mediathekview.mserver.ui.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.mediathekview.mserver.ui.config.MServerCommandLine.CMDARG;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MServerExecutionFlowTest {

  @Test
  void startRejectsUnknownFlow() {
    final MServerExecutionFlow executionFlow =
        new MServerExecutionFlow(null, Map.of(CMDARG.flow, "checkAvailabilty"));

    assertThatThrownBy(executionFlow::start)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown flow");
  }
}
