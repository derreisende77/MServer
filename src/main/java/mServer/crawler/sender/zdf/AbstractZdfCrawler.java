package mServer.crawler.sender.zdf;

import de.mediathekview.mlib.Const;
import de.mediathekview.mlib.daten.DatenFilm;
import de.mediathekview.mlib.tool.Log;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import mServer.crawler.FilmeSuchen;
import mServer.crawler.sender.MediathekCrawler;
import mServer.crawler.sender.base.CrawlerUrlDTO;
import mServer.crawler.sender.base.JsoupConnection;
import mServer.crawler.sender.zdf.tasks.ZdfDayPageTask;
import mServer.crawler.sender.zdf.tasks.ZdfFilmDetailTask;
import mServer.crawler.sender.zdf.tasks.ZdfIndexPageTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractZdfCrawler extends MediathekCrawler {

  private static final Logger LOG = LogManager.getLogger(AbstractZdfCrawler.class);

  JsoupConnection jsoupConnection = new JsoupConnection();

  public AbstractZdfCrawler(String sender, FilmeSuchen ssearch, int startPrio) {
    super(ssearch, sender, 0, 1, startPrio);
  }

  @Override
  protected RecursiveTask<Set<DatenFilm>> createCrawlerTask() {

    try {
      final ZdfConfiguration configuration = loadConfiguration();

      final ConcurrentLinkedQueue<CrawlerUrlDTO> shows
              = new ConcurrentLinkedQueue<>(getDaysEntries(configuration));

      Log.sysLog(getSendername() + " Anzahl: " + shows.size());
      meldungAddMax(shows.size());

      return new ZdfFilmDetailTask(this, getApiUrlBase(), shows, configuration.getVideoAuthKey());
    } catch (final InterruptedException ex) {
      LOG.debug("{} crawler interrupted.", getSendername(), ex);
      Thread.currentThread().interrupt();
    } catch (final ExecutionException ex) {
      LOG.fatal("Exception in {} crawler.", getSendername(), ex);
    }
    return null;
  }

  protected ZdfConfiguration loadConfiguration() throws ExecutionException, InterruptedException {
    final ZdfIndexPageTask task = new ZdfIndexPageTask(this, getUrlBase(), jsoupConnection);
    return forkJoinPool.submit(task).get();
  }

  protected abstract String getUrlBase();

  private Set<CrawlerUrlDTO> getDaysEntries(final ZdfConfiguration configuration)
          throws InterruptedException, ExecutionException {
    final ZdfDayPageTask dayTask
            = new ZdfDayPageTask(this, getApiUrlBase(), getDayUrls(), configuration.getSearchAuthKey());
    final Set<CrawlerUrlDTO> shows = forkJoinPool.submit(dayTask).get();

    Collection<? extends CrawlerUrlDTO> extraDaysEntries = getExtraDaysEntries();
    shows.addAll(extraDaysEntries);

    Log.sysLog(getSendername() + ": days entries: " + shows.size());

    return shows;
  }

  protected Collection<CrawlerUrlDTO> getExtraDaysEntries()
          throws ExecutionException, InterruptedException {
    return new HashSet<>();
  }

  protected abstract String getApiUrlBase();

  private ConcurrentLinkedQueue<CrawlerUrlDTO> getDayUrls() {
    final ConcurrentLinkedQueue<CrawlerUrlDTO> urls = new ConcurrentLinkedQueue<>();
    for (int i = 0;
            i
            <= crawlerConfig.getMaximumDaysForSendungVerpasstSection()
            + crawlerConfig.getMaximumDaysForSendungVerpasstSectionFuture();
            i++) {

      final LocalDateTime local
              = LocalDateTime.now()
                      .plus(crawlerConfig.getMaximumDaysForSendungVerpasstSectionFuture(), ChronoUnit.DAYS)
                      .minus(i, ChronoUnit.DAYS);
      final String date = local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      final String url = String.format(getUrlDay(), date, date);
      urls.add(new CrawlerUrlDTO(url));
    }

    return urls;
  }

  protected abstract String getUrlDay();
}
