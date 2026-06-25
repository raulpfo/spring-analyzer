package io.github.springanalyzer.commands.ui;

import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Help.Ansi;

@Component
public class ProgressBar {
  private volatile boolean running = false;

  public void start(final String message, final String color) {
    running = true;
    Thread.ofVirtual().start(() -> {
      final String[] frames = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
      final String resolvedColor = StringUtils.isBlank(color) ? "cyan" : color.trim();
      int i = 0;
      while (running) {
        final String ansiString = new StringBuilder()
            .append("@|")
            .append(resolvedColor)
            .append(" ")
            .append(frames[i % frames.length])
            .append("|@")
            .toString();

        final String frame = new StringBuilder()
            .append("\r")
            .append(Ansi.AUTO.string(ansiString))
            .append(" ")
            .append(message)
            .toString();
        System.out.print(frame);
        i++;
        try { Thread.sleep(80); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
      }
    });
  }

  public void stop(String doneMessage) {
    running = false;
    System.out.println("\r" + Ansi.AUTO.string("@|green ✅ |@") + " " + doneMessage);
  }
}
