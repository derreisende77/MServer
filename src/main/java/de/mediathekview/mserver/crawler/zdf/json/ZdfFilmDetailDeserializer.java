package de.mediathekview.mserver.crawler.zdf.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.mediathekview.mlib.daten.Film;
import de.mediathekview.mlib.daten.Sender;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ZdfFilmDetailDeserializer implements JsonDeserializer<Optional<Film>> {

  private static final Logger LOG = LogManager.getLogger(ZdfFilmDetailDeserializer.class);

  private static final String JSON_ELEMENT_BEGIN = "airtimeBegin";
  private static final String JSON_ELEMENT_BRAND = "http://zdf.de/rels/brand";
  private static final String JSON_ELEMENT_CATEGORY = "http://zdf.de/rels/category";
  private static final String JSON_ELEMENT_BROADCAST = "http://zdf.de/rels/cmdm/broadcasts";
  private static final String JSON_ELEMENT_DURATION = "duration";
  private static final String JSON_ELEMENT_EDITORIAL_DATE = "editorialDate";
  private static final String JSON_ELEMENT_LEAD_PARAGRAPH = "leadParagraph";
  private static final String JSON_ELEMENT_MAIN_VIDEO = "mainVideoContent";
  private static final String JSON_ELEMENT_PROGRAM_ITEM = "programmeItem";
  private static final String JSON_ELEMENT_SHARING_URL = "http://zdf.de/rels/sharing-url";
  private static final String JSON_ELEMENT_SUBTITLE = "subtitle";
  private static final String JSON_ELEMENT_TARGET = "http://zdf.de/rels/target";
  private static final String JSON_ELEMENT_TITLE = "title";
  private static final String JSON_ELEMENT_TEASER_TEXT = "teasertext";

  private static final DateTimeFormatter DATE_FORMATTER_EDITORIAL = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");//2016-10-29T16:15:00.000+02:00
  private static final DateTimeFormatter DATE_FORMATTER_AIRTIME = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");//2016-10-29T16:15:00+02:00

  @Override
  public Optional<Film> deserialize(JsonElement aJsonObject, Type aType, JsonDeserializationContext aContext) {
    JsonObject rootNode = aJsonObject.getAsJsonObject();
    JsonObject programItemTarget = null;

    if (rootNode.has(JSON_ELEMENT_PROGRAM_ITEM) && !rootNode.get(JSON_ELEMENT_PROGRAM_ITEM).isJsonNull()) {
      JsonArray programItem = rootNode.getAsJsonArray(JSON_ELEMENT_PROGRAM_ITEM);
      programItemTarget = programItem.get(0).getAsJsonObject().get(JSON_ELEMENT_TARGET).getAsJsonObject();
    }

    Optional<String> title = parseTitle(rootNode, programItemTarget);
    Optional<String> topic = parseTopic(rootNode);
    Optional<String> description = parseDescription(rootNode);

    Optional<String> website = parseWebsiteUrl(rootNode);
    Optional<LocalDateTime> time = parseAirtime(rootNode, programItemTarget);
    Optional<Duration> duration = parseDuration(rootNode);

    if (title.isPresent()) {
      return createFilm(topic, title.get(), description, website, time, duration);
    } else {
      LOG.error("ZdfFilmDetailDeserializer: no title found");
    }

    return Optional.empty();
  }

  private Optional<Film> createFilm(
      final Optional<String> aTopic,
      final String aTitle,
      final Optional<String> aDescription,
      final Optional<String> aWebsite,
      final Optional<LocalDateTime> aTime,
      final Optional<Duration> aDuration) {

    try {
      final Film film
          = new Film(
          UUID.randomUUID(),
          Sender.ZDF,
          aTitle,
          aTopic.orElse(aTitle),
          aTime.orElse(LocalDateTime.now()),
          aDuration.orElse(Duration.ZERO));

      if (aWebsite.isPresent()) {
        film.setWebsite(new URL(aWebsite.get()));
      }
      if (aDescription.isPresent()) {
        film.setBeschreibung(aDescription.get());
      }

      return Optional.of(film);
    } catch (MalformedURLException ex) {
      LOG.fatal("ZdfFilmDeserializer: url can't be parsed.", ex);
    }

    return Optional.empty();
  }

  private Optional<LocalDateTime> parseAirtime(JsonObject aRootNode, JsonObject aProgramItemTarget) {
    Optional<String> date;
    DateTimeFormatter formatter;

    // use broadcast airtime if found
    if (aProgramItemTarget != null) {
      JsonArray broadcastArray = aProgramItemTarget.getAsJsonArray(JSON_ELEMENT_BROADCAST);

      if (broadcastArray == null || broadcastArray.size() < 1) {
        date = getEditorialDate(aRootNode);
        formatter = DATE_FORMATTER_EDITORIAL;
      } else {
        // array is ordered ascending though the oldest broadcast is the first entry
        date = Optional.of(broadcastArray.get(0).getAsJsonObject().get(JSON_ELEMENT_BEGIN).getAsString());
        formatter = DATE_FORMATTER_AIRTIME;
      }
    } else {
      // use editorialdate
      date = getEditorialDate(aRootNode);
      formatter = DATE_FORMATTER_EDITORIAL;
    }

    if (date.isPresent()) {
      return Optional.of(LocalDateTime.parse(date.get(), formatter));
    }

    return Optional.empty();
  }

  private Optional<String> getEditorialDate(JsonObject aRootNode) {
    if (aRootNode.has(JSON_ELEMENT_EDITORIAL_DATE)) {
      return Optional.of(aRootNode.get(JSON_ELEMENT_EDITORIAL_DATE).getAsString());
    }

    return Optional.empty();
  }

  private Optional<String> parseWebsiteUrl(JsonObject aRootNode) {
    if (aRootNode.has(JSON_ELEMENT_SHARING_URL)) {
      return Optional.of(aRootNode.get(JSON_ELEMENT_SHARING_URL).getAsString());
    }

    return Optional.empty();
  }

  private Optional<Duration> parseDuration(JsonObject aRootNode) {
    JsonElement mainVideoElement = aRootNode.get(JSON_ELEMENT_MAIN_VIDEO);
    if (mainVideoElement != null) {
      JsonObject mainVideo = mainVideoElement.getAsJsonObject();
      JsonObject targetMainVideo = mainVideo.get(JSON_ELEMENT_TARGET).getAsJsonObject();
      JsonElement duration = targetMainVideo.get(JSON_ELEMENT_DURATION);
      if (duration != null) {
        return Optional.of(Duration.ofSeconds(duration.getAsInt()));
      }
    }

    return Optional.empty();
  }

  private Optional<String> parseDescription(JsonObject aRootNode) {
    JsonElement leadParagraph = aRootNode.get(JSON_ELEMENT_LEAD_PARAGRAPH);
    if (leadParagraph != null) {
      return Optional.of(leadParagraph.getAsString());
    } else {
      JsonElement teaserText = aRootNode.get(JSON_ELEMENT_TEASER_TEXT);
      if (teaserText != null) {
        return Optional.of(teaserText.getAsString());
      }
    }

    return Optional.empty();
  }

  private Optional<String> parseTitle(JsonObject aRootNode, JsonObject aTarget) {
    // use property "title" if found
    JsonElement titleElement = aRootNode.get(JSON_ELEMENT_TITLE);
    if (titleElement != null) {
      JsonElement subTitleElement = aRootNode.get(JSON_ELEMENT_SUBTITLE);
      if (subTitleElement != null) {
        return Optional.of(titleElement.getAsString() + " - " + subTitleElement.getAsString());
      } else {
        return Optional.of(titleElement.getAsString());
      }
    } else {
      // programmItem target required to determine title
      if (aTarget != null && aTarget.has(JSON_ELEMENT_TITLE)) {
        String title = aTarget.get(JSON_ELEMENT_TITLE).getAsString();
        String subTitle = aTarget.get(JSON_ELEMENT_SUBTITLE).getAsString();

        if (subTitle.isEmpty()) {
          return Optional.of(title);
        } else {
          return Optional.of(title + " - " + subTitle);
        }
      }
    }

    return Optional.empty();
  }

  private Optional<String> parseTopic(JsonObject aRootNode) {
    JsonObject brand = aRootNode.getAsJsonObject(JSON_ELEMENT_BRAND);
    JsonObject category = aRootNode.getAsJsonObject(JSON_ELEMENT_CATEGORY);

    if (brand != null) {
      // first use brand
      JsonElement topic = brand.get(JSON_ELEMENT_TITLE);
      if (topic != null) {
        return Optional.of(topic.getAsString());
      }
    }

    if (category != null) {
      // second use category
      JsonElement topic = category.get(JSON_ELEMENT_TITLE);
      if (topic != null) {
        return Optional.of(topic.getAsString());
      }
    }

    return Optional.empty();
  }
}