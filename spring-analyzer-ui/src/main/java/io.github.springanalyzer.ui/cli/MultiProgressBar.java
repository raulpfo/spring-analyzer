package io.github.springanalyzer.commands.ui;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Help.Ansi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MultiProgressBar {
  private static final String[] FRAMES = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
  private static final int lineWidth = 50;

  private final Map<String, RepoState> states = Collections.synchronizedMap(new LinkedHashMap<>());
  private final Map<String, Long> startTimes = Collections.synchronizedMap(new LinkedHashMap<>());
  private final Map<String, Long> endTimes = Collections.synchronizedMap(new LinkedHashMap<>());

  private volatile boolean running = false;

  private int repoCount = 0;

  public void start(final List<String> repoNames) {
    repoNames.forEach(name -> {
      states.put(name, RepoState.CLONING);
      startTimes.put(name, System.currentTimeMillis());
    });
    repoCount = repoNames.size();

    repoNames.forEach(name -> System.out.println());

    running = true;
    Thread refreshThread = Thread.ofVirtual().start(this::refresh);
  }

  public void done(final String repoName) {
    endTimes.put(repoName, System.currentTimeMillis());
    states.put(repoName, RepoState.DONE);
  }

  public void error(final String repoName) {
    endTimes.put(repoName, System.currentTimeMillis());
    states.put(repoName, RepoState.ERROR);
  }

  public void stop() {
    running = false;
    refresh();
    System.out.println();
  }

  private void refresh() {
    int frame = 0;
    while (running) {
      repaint(frame++);
      try { Thread.sleep(80); }
      catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    repaint(0);
  }

  private void repaint(final int frame) {
    final StringBuilder sb = new StringBuilder();

    for (int i = 0; i < repoCount; i++) {
      sb.append("\033[A");
    }

    states.forEach((name, state) -> {
      sb.append("\033[2K\r");
      sb.append(renderLine(name, state, frame));
      sb.append("\n");
    });

    System.out.print(sb);
  }

  private String renderLine(final String name, final RepoState state, final int frame) {
    final long elapsedMs = switch (state) {
      case CLONING -> System.currentTimeMillis() - startTimes.get(name);
      case DONE, ERROR -> endTimes.getOrDefault(name, System.currentTimeMillis()) - startTimes.get(name);
    };

    final int dots = lineWidth - name.length();
    final String padding = ".".repeat(Math.max(0, dots));

    final StringBuilder sb = new StringBuilder()
        .append("- ")
        .append(name)
        .append(" ")
        .append(padding)
        .append(" ");

    return switch (state) {
      case CLONING -> sb.append(Ansi.AUTO.string(
          new StringBuilder()
              .append("@|cyan ")
              .append(FRAMES[frame % FRAMES.length])
              .append("|@")
              .toString()
      )).toString();

      case DONE -> sb.append(Ansi.AUTO.string("@|green DONE!|@"))
          .append(" [ ")
          .append(String.format("%5.3f s", elapsedMs / 1000.0))
          .append(" ]")
          .toString();

      case ERROR -> sb.append(Ansi.AUTO.string("@|red FAILED |@"))
          .append(" [ ")
          .append(String.format("%5.3f s", elapsedMs / 1000.0))
          .append(" ]")
          .toString();
    };
  }

  public enum RepoState {
    CLONING, DONE, ERROR
  }
}