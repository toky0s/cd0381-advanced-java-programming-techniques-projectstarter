package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

public class CustomRecursiveTask extends RecursiveTask<Boolean> {
  private final Clock clock;
  private final ConcurrentMap<String, Integer> counts;
  private final Instant deadline;
  private final List<Pattern> ignoredUrls;
  private final int maxDepth;
  private final PageParserFactory parserFactory;
  private final String url;
  private final ConcurrentSkipListSet<String> visitedUrls;

  public CustomRecursiveTask(
      String url,
      Instant deadline,
      int maxDepth,
      ConcurrentMap<String, Integer> counts,
      ConcurrentSkipListSet<String> visitedUrls,
      Clock clock,
      PageParserFactory parserFactory,
      List<Pattern> ignoredUrls) {
    this.counts = counts;
    this.clock = clock;
    this.url = url;
    this.deadline = deadline;
    this.ignoredUrls = ignoredUrls;
    this.maxDepth = maxDepth;
    this.parserFactory = parserFactory;
    this.visitedUrls = visitedUrls;
  }

  @Override
  protected Boolean compute() {
    if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
      return false;
    }
    for (Pattern pattern : ignoredUrls) {
      if (pattern.matcher(url).matches()) {
        return false;
      }
    }

    synchronized (this) {
      if (visitedUrls.contains(url)) {
        return false;
      }
      if (!visitedUrls.add(url)) {
        return false;
      }
    }

    PageParser.Result result = parserFactory.get(url).parse();
    for (ConcurrentMap.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
      counts.compute(e.getKey(), (k, v) -> (v == null) ? e.getValue() : e.getValue() + v);
    }

    List<CustomRecursiveTask> customRecursiveTasks = new ArrayList<>();
    for (String link : result.getLinks()) {
      customRecursiveTasks.add(new CustomRecursiveTask(link, deadline, maxDepth - 1, counts,
          visitedUrls, clock, parserFactory, ignoredUrls));
    }
    invokeAll(customRecursiveTasks);
    return true;
  }
}
