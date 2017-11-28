package de.mediathekview.mserver.crawler.zdf.json;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.mediathekview.mlib.daten.GeoLocations;
import de.mediathekview.mlib.daten.Resolution;

/**
 * A JSON deserializer to gather the needed information for a {@link DownloadDTO}.
 */
public class ZDFDownloadDTODeserializer implements JsonDeserializer<Optional<DownloadDTO>> {
  private static final String ZDF_QUALITY_VERYHIGH = "veryhigh";
  private static final String ZDF_QUALITY_HIGH = "high";
  private static final String ZDF_QUALITY_MED = "med";
  private static final String ZDF_QUALITY_LOW = "low";
  private static final Logger LOG = LogManager.getLogger(ZDFDownloadDTODeserializer.class);
  private static final String JSON_ELEMENT_ATTRIBUTES = "attributes";
  private static final String JSON_ELEMENT_AUDIO = "audio";
  private static final String JSON_ELEMENT_CAPTIONS = "captions";
  private static final String JSON_ELEMENT_FORMITAET = "formitaeten";
  private static final String JSON_ELEMENT_GEOLOCATION = "geoLocation";
  private static final String JSON_ELEMENT_HD = "hd";
  private static final String JSON_ELEMENT_MIMETYPE = "mimeType";
  private static final String JSON_ELEMENT_PRIORITYLIST = "priorityList";
  private static final String JSON_ELEMENT_QUALITY = "quality";
  private static final String JSON_ELEMENT_TRACKS = "tracks";
  private static final String JSON_ELEMENT_URI = "uri";

  private static final String JSON_PROPERTY_VALUE = "value";


  private static final String RELEVANT_MIME_TYPE = "video/mp4";
  private static final String RELEVANT_SUBTITLE_TYPE = ".xml";
  private static final String JSON_ELEMENT_QUALITIES = "qualities";

  @Override
  public Optional<DownloadDTO> deserialize(final JsonElement aJsonElement, final Type aTypeOfT,
      final JsonDeserializationContext aJsonDeserializationContext) {
    final DownloadDTO dto = new DownloadDTO();
    try {
      final JsonObject rootNode = aJsonElement.getAsJsonObject();

      parseVideoUrls(dto, rootNode);
      parseSubtitle(dto, rootNode);
      parseGeoLocation(dto, rootNode);

      return Optional.of(dto);
    } catch (final UnsupportedOperationException unsupportedOperationException) {
      // This will happen when a element is JsonNull.
      LOG.error("BR: A needed JSON element is JsonNull.", unsupportedOperationException);
    }

    return Optional.empty();
  }

  private void parseFormitaet(final DownloadDTO dto, final JsonElement formitaet) {
    // only mp4-videos are relevant
    final JsonElement mimeType = formitaet.getAsJsonObject().get(JSON_ELEMENT_MIMETYPE);
    if (mimeType != null && mimeType.getAsString().equalsIgnoreCase(RELEVANT_MIME_TYPE)) {

      // array Resolution
      final JsonArray qualityList =
          formitaet.getAsJsonObject().getAsJsonArray(JSON_ELEMENT_QUALITIES);
      for (final JsonElement quality : qualityList) {
        String uri = null;

        final Resolution qualityValue = parseVideoQuality(quality.getAsJsonObject());

        // subelement audio
        final JsonElement audio = quality.getAsJsonObject().get(JSON_ELEMENT_AUDIO);
        if (audio != null) {

          // array tracks
          final JsonArray tracks = audio.getAsJsonObject().getAsJsonArray(JSON_ELEMENT_TRACKS);

          final JsonObject track = tracks.get(0).getAsJsonObject();
          uri = track.get(JSON_ELEMENT_URI).getAsString();
        }

        if (qualityValue != null && uri != null) {
          dto.addUrl(qualityValue, uri);
        }
      }
    }
  }

  private void parseGeoLocation(final DownloadDTO dto, final JsonObject rootNode) {
    final JsonElement attributes = rootNode.get(JSON_ELEMENT_ATTRIBUTES);
    if (attributes != null) {
      final JsonElement geoLocation = attributes.getAsJsonObject().get(JSON_ELEMENT_GEOLOCATION);
      if (geoLocation != null) {
        final JsonElement geoValue = geoLocation.getAsJsonObject().get(JSON_PROPERTY_VALUE);
        if (geoValue != null) {
          final Optional<GeoLocations> foundGeoLocation = GeoLocations.find(geoValue.getAsString());
          if (foundGeoLocation.isPresent()) {
            dto.setGeoLocation(foundGeoLocation.get());
          } else {
            LOG.debug(String.format("Can't find a GeoLocation for \"%s", geoValue.getAsString()));
          }
        }
      }
    }
  }

  private void parsePriority(final DownloadDTO dto, final JsonElement priority) {
    if (priority != null) {

      // array formitaeten
      final JsonArray formitaetList =
          priority.getAsJsonObject().getAsJsonArray(JSON_ELEMENT_FORMITAET);
      for (final JsonElement formitaet : formitaetList) {
        parseFormitaet(dto, formitaet);
      }
    }
  }

  private void parseSubtitle(final DownloadDTO dto, final JsonObject rootNode) {
    final JsonArray captionList = rootNode.getAsJsonArray(JSON_ELEMENT_CAPTIONS);
    final Iterator<JsonElement> captionIterator = captionList.iterator();
    while (captionIterator.hasNext()) {
      final JsonObject caption = captionIterator.next().getAsJsonObject();
      final JsonElement uri = caption.get(JSON_ELEMENT_URI);
      if (uri != null) {
        final String uriValue = uri.getAsString();

        // prefer xml subtitles
        if (uriValue.endsWith(RELEVANT_SUBTITLE_TYPE)) {
          dto.setSubTitleUrl(uriValue);
          break;
        } else if (dto.getSubTitleUrl().isPresent()) {
          dto.setSubTitleUrl(uriValue);
        }

      }
    }
  }

  private Resolution parseVideoQuality(final JsonObject quality) {
    Resolution qualityValue = null;
    final JsonElement hd = quality.get(JSON_ELEMENT_HD);
    if (hd != null && hd.getAsBoolean()) {
      qualityValue = Resolution.HD;
    } else {
      final String zdfQuality = quality.get(JSON_ELEMENT_QUALITY).getAsString();
      switch (zdfQuality) {
        case ZDF_QUALITY_LOW:
          qualityValue = Resolution.SMALL;
          break;
        case ZDF_QUALITY_MED:
          qualityValue = Resolution.SMALL;
          break;
        case ZDF_QUALITY_HIGH:
          qualityValue = Resolution.SMALL;
          break;
        case ZDF_QUALITY_VERYHIGH:
          qualityValue = Resolution.NORMAL;
          break;
        default:
          qualityValue = Resolution.VERY_SMALL;
      }
    }
    return qualityValue;
  }

  private void parseVideoUrls(final DownloadDTO dto, final JsonObject rootNode) {
    // array priorityList
    final JsonArray priorityList = rootNode.getAsJsonArray(JSON_ELEMENT_PRIORITYLIST);
    for (final JsonElement priority : priorityList) {

      parsePriority(dto, priority);
    }
  }
}
