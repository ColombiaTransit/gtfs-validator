package org.mobilitydata.gtfsvalidator.report.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.flogger.FluentLogger;
import com.vladsch.flexmark.util.misc.Pair;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import org.mobilitydata.gtfsvalidator.table.*;
import org.mobilitydata.gtfsvalidator.util.CalendarUtil;
import org.mobilitydata.gtfsvalidator.util.ServicePeriod;

public class FeedMetadata {
  /*
   * Use these strings as keys in the FeedInfo map. Also used to specify the info that will appear
   * in the json report. Adding elements to feedInfo will not automatically be included in the json
   * report and should be explicitly handled in the json report code.
   */
  public static final String FEED_INFO_PUBLISHER_NAME = "Publisher Name";
  public static final String FEED_INFO_PUBLISHER_URL = "Publisher URL";
  public static final String FEED_INFO_FEED_CONTACT_EMAIL = "Feed Email";
  public static final String FEED_INFO_FEED_LANGUAGE = "Feed Language";
  public static final String FEED_INFO_FEED_START_DATE = "Feed Start Date";
  public static final String FEED_INFO_FEED_END_DATE = "Feed End Date";
  public static final String FEED_INFO_SERVICE_WINDOW = "Service Window";
  public static final String FEED_INFO_SERVICE_WINDOW_START = "Service Window Start";
  public static final String FEED_INFO_SERVICE_WINDOW_END = "Service Window End";

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  /*
   * Use these strings as keys in the counts map. Also used to specify the info that will appear in
   * the json report. Adding elements to feedInfo will not automatically be included in the json
   * report and should be explicitly handled in the json report code.
   */
  public static final String COUNTS_SHAPES = "Shapes";
  public static final String COUNTS_STOPS = "Stops";
  public static final String COUNTS_ROUTES = "Routes";
  public static final String COUNTS_TRIPS = "Trips";
  public static final String COUNTS_AGENCIES = "Agencies";
  public static final String COUNTS_BLOCKS = "Blocks";

  private Map<String, TableMetadata> tableMetaData;
  public Map<String, Integer> counts = new TreeMap<>();

  public Map<String, String> feedInfo = new LinkedHashMap<>();

  public Map<String, Boolean> specFeatures = new LinkedHashMap<>();

  public ArrayList<AgencyMetadata> agencies = new ArrayList<>();
  private ImmutableSortedSet<String> filenames;

  public double validationTimeSeconds;

  // List of features that only require checking the presence of one record in the file.
  private final List<Pair<String, String>> FILE_BASED_FEATURES =
      List.of(
          new Pair<>("Pathways (basic)", GtfsPathway.FILENAME),
          new Pair<>("Transfers", GtfsTransfer.FILENAME),
          new Pair<>("Fares V1", GtfsFareAttribute.FILENAME),
          new Pair<>("Fare Products", GtfsFareProduct.FILENAME),
          new Pair<>("Shapes", GtfsShape.FILENAME),
          new Pair<>("Frequencies", GtfsFrequency.FILENAME),
          new Pair<>("Feed Information", GtfsFeedInfo.FILENAME),
          new Pair<>("Attributions", GtfsAttribution.FILENAME),
          new Pair<>("Translations", GtfsTranslation.FILENAME),
          new Pair<>("Fare Media", GtfsFareMedia.FILENAME),
          new Pair<>("Zone-Based Fares", GtfsArea.FILENAME),
          new Pair<>("Transfer Fares", GtfsFareTransferRule.FILENAME),
          new Pair<>("Time-Based Fares", GtfsTimeframe.FILENAME),
          new Pair<>("Levels", GtfsLevel.FILENAME),
          new Pair<>("Booking Rules", GtfsBookingRules.FILENAME),
          new Pair<>("Fixed-Stops Demand Responsive Transit", GtfsLocationGroups.FILENAME));

  protected FeedMetadata() {}

  public static FeedMetadata from(GtfsFeedContainer feedContainer, ImmutableSet<String> filenames) {
    var feedMetadata = new FeedMetadata();
    feedMetadata.setFilenames(ImmutableSortedSet.copyOf(filenames));
    TreeMap<String, TableMetadata> map = new TreeMap<>();
    for (var table : feedContainer.getTables()) {
      var metadata = TableMetadata.from(table);
      map.put(metadata.getFilename(), metadata);
    }
    feedMetadata.setTableMetaData(map);

    feedMetadata.setCounts(feedContainer);

    if (feedContainer.getTableForFilename(GtfsFeedInfo.FILENAME).isPresent()) {
      feedMetadata.loadFeedInfo(
          (GtfsTableContainer<GtfsFeedInfo>)
              feedContainer.getTableForFilename(GtfsFeedInfo.FILENAME).get());
    }

    feedMetadata.loadAgencyData(
        (GtfsTableContainer<GtfsAgency>)
            feedContainer.getTableForFilename(GtfsAgency.FILENAME).get());

    if (feedContainer.getTableForFilename(GtfsTrip.FILENAME).isPresent()
        && (feedContainer.getTableForFilename(GtfsCalendar.FILENAME).isPresent()
            || feedContainer.getTableForFilename(GtfsCalendarDate.FILENAME).isPresent())) {
      feedMetadata.loadServiceWindow(
          (GtfsTableContainer<GtfsTrip>) feedContainer.getTableForFilename(GtfsTrip.FILENAME).get(),
          (GtfsTableContainer<GtfsCalendar>)
              feedContainer.getTableForFilename(GtfsCalendar.FILENAME).get(),
          (GtfsTableContainer<GtfsCalendarDate>)
              feedContainer.getTableForFilename(GtfsCalendarDate.FILENAME).get());
    }

    feedMetadata.loadSpecFeatures(feedContainer);
    return feedMetadata;
  }

  private void setCounts(GtfsFeedContainer feedContainer) {
    setCount(COUNTS_SHAPES, feedContainer, GtfsShape.FILENAME, GtfsShape.class, GtfsShape::shapeId);
    setCount(COUNTS_STOPS, feedContainer, GtfsStop.FILENAME, GtfsStop.class, GtfsStop::stopId);
    setCount(COUNTS_ROUTES, feedContainer, GtfsRoute.FILENAME, GtfsRoute.class, GtfsRoute::routeId);
    setCount(COUNTS_TRIPS, feedContainer, GtfsTrip.FILENAME, GtfsTrip.class, GtfsTrip::tripId);
    setCount(
        COUNTS_AGENCIES,
        feedContainer,
        GtfsAgency.FILENAME,
        GtfsAgency.class,
        GtfsAgency::agencyId);
    setCount(COUNTS_BLOCKS, feedContainer, GtfsTrip.FILENAME, GtfsTrip.class, GtfsTrip::blockId);
  }

  private <T extends GtfsTableContainer<E>, E extends GtfsEntity> void setCount(
      String countName,
      GtfsFeedContainer feedContainer,
      String fileName,
      Class<E> clazz,
      Function<E, String> idExtractor) {

    var table = feedContainer.getTableForFilename(fileName);
    this.counts.put(
        countName,
        table
            .map(gtfsTableContainer -> loadUniqueCount(gtfsTableContainer, clazz, idExtractor))
            .orElse(0));
  }

  private <E extends GtfsEntity> int loadUniqueCount(
      GtfsTableContainer<?> table, Class<E> clazz, Function<E, String> idExtractor) {
    // Iterate through entities and count unique IDs
    Set<String> uniqueIds = new HashSet<>();
    for (GtfsEntity entity : table.getEntities()) {
      if (entity != null) {
        E castedEntity = clazz.cast(entity);
        String id = idExtractor.apply(castedEntity);
        if (id != null && !id.isEmpty()) {
          uniqueIds.add(id);
        }
      }
    }
    return uniqueIds.size();
  }

  private void loadSpecFeatures(GtfsFeedContainer feedContainer) {
    loadSpecFeaturesBasedOnFilePresence(feedContainer);
    loadSpecFeaturesBasedOnFieldPresence(feedContainer);
  }

  private void loadSpecFeaturesBasedOnFilePresence(GtfsFeedContainer feedContainer) {
    for (Pair<String, String> entry : FILE_BASED_FEATURES) {
      specFeatures.put(entry.getKey(), hasAtLeastOneRecordInFile(feedContainer, entry.getValue()));
    }
  }

  private void loadSpecFeaturesBasedOnFieldPresence(GtfsFeedContainer feedContainer) {
    loadRouteColorsFeature(feedContainer);
    loadHeadsignsFeature(feedContainer);
    loadWheelchairAccessibilityFeature(feedContainer);
    loadTTSFeature(feedContainer);
    loadBikeAllowanceFeature(feedContainer);
    loadLocationTypesFeature(feedContainer);
    loadTraversalTimeFeature(feedContainer);
    loadPathwayDirectionsFeature(feedContainer);
    loadPathwayExtraFeature(feedContainer);
    loadRouteBasedFaresFeature(feedContainer);
    loadContinuousStopsFeature(feedContainer);
    loadZoneBasedDemandResponsiveTransitFeature(feedContainer);
    loadDeviatedFixedRouteFeature(feedContainer);
  }

  private void loadDeviatedFixedRouteFeature(GtfsFeedContainer feedContainer) {
    specFeatures.put("Deviated Fixed Route", hasAtLeastOneTripWithAllFields(feedContainer));
  }

  private boolean hasAtLeastOneTripWithAllFields(GtfsFeedContainer feedContainer) {
    Optional<GtfsTableContainer<?>> optionalStopTimeTable =
        feedContainer.getTableForFilename(GtfsStopTime.FILENAME);
    if (optionalStopTimeTable.isPresent()) {
      for (GtfsEntity entity : optionalStopTimeTable.get().getEntities()) {
        if (entity instanceof GtfsStopTime) {
          GtfsStopTime stopTime = (GtfsStopTime) entity;
          return stopTime.hasTripId()
              && stopTime.tripId() != null
              && stopTime.hasLocationId()
              && stopTime.locationId() != null
              && stopTime.hasStopId()
              && stopTime.stopId() != null
              && stopTime.hasArrivalTime()
              && stopTime.arrivalTime() != null
              && stopTime.hasDepartureTime()
              && stopTime.departureTime() != null;
        }
      }
    }
    return false;
  }

  private void loadZoneBasedDemandResponsiveTransitFeature(GtfsFeedContainer feedContainer) {
    specFeatures.put(
        "Zone-Based Demand Responsive Transit", hasAtLeastOneTripWithOnlyLocationId(feedContainer));
  }

  private boolean hasAtLeastOneTripWithOnlyLocationId(GtfsFeedContainer feedContainer) {
    Optional<GtfsTableContainer<?>> optionalStopTimeTable =
        feedContainer.getTableForFilename(GtfsStopTime.FILENAME);
    if (optionalStopTimeTable.isPresent()) {
      for (GtfsEntity entity : optionalStopTimeTable.get().getEntities()) {
        if (entity instanceof GtfsStopTime) {
          GtfsStopTime stopTime = (GtfsStopTime) entity;
          if (stopTime.hasTripId()
              && stopTime.tripId() != null
              && stopTime.hasLocationId()
              && stopTime.locationId() != null
              && (!stopTime.hasStopId() || stopTime.stopId() == null)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private void loadContinuousStopsFeature(GtfsFeedContainer feedContainer) {
    specFeatures.put(
        "Continuous Stops",
        hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsRoute.FILENAME,
                List.of((Function<GtfsRoute, Boolean>) GtfsRoute::hasContinuousDropOff))
            || hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsRoute.FILENAME,
                List.of((Function<GtfsRoute, Boolean>) GtfsRoute::hasContinuousPickup))
            || hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsStopTime.FILENAME,
                List.of((Function<GtfsStopTime, Boolean>) GtfsStopTime::hasContinuousDropOff))
            || hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsStopTime.FILENAME,
                List.of((Function<GtfsStopTime, Boolean>) GtfsStopTime::hasContinuousPickup)));
  }

  private void loadRouteBasedFaresFeature(GtfsFeedContainer feedContainer) {
    specFeatures.put(
        "Route-Based Fares",
        hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsRoute.FILENAME,
                List.of((Function<GtfsRoute, Boolean>) GtfsRoute::hasNetworkId))
            || hasAtLeastOneRecordInFile(feedContainer, GtfsNetwork.FILENAME));
  }

  private void loadPathwayDirectionsFeature(GtfsFeedContainer feedContainer) {
    specFeatures.put(
        "Pathways Directions",
        hasAtLeastOneRecordForFields(
            feedContainer,
            GtfsPathway.FILENAME,
            List.of(
                GtfsPathway::hasSignpostedAs,
                (Function<GtfsPathway, Boolean>) GtfsPathway::hasReversedSignpostedAs)));
  }

  private void loadPathwayExtraFeature(GtfsFeedContainer feedContainer) {
    specFeatures.put(
        "Pathways (extra)",
        hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsPathway.FILENAME,
                List.of((Function<GtfsPathway, Boolean>) GtfsPathway::hasMaxSlope))
            || hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsPathway.FILENAME,
                List.of((Function<GtfsPathway, Boolean>) GtfsPathway::hasMinWidth))
            || hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsPathway.FILENAME,
                List.of((Function<GtfsPathway, Boolean>) GtfsPathway::hasLength))
            || hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsPathway.FILENAME,
                List.of((Function<GtfsPathway, Boolean>) GtfsPathway::hasStairCount)));
  }

  private void loadTraversalTimeFeature(GtfsFeedContainer feedContainer) {
    specFeatures.put(
        "Traversal Time",
        hasAtLeastOneRecordForFields(
            feedContainer,
            GtfsPathway.FILENAME,
            List.of((Function<GtfsPathway, Boolean>) GtfsPathway::hasTraversalTime)));
  }

  private void loadLocationTypesFeature(GtfsFeedContainer feedContainer) {
    specFeatures.put(
        "Location Types",
        hasAtLeastOneRecordForFields(
            feedContainer,
            GtfsStop.FILENAME,
            List.of((Function<GtfsStop, Boolean>) GtfsStop::hasLocationType)));
  }

  private void loadBikeAllowanceFeature(GtfsFeedContainer feedContainer) {
    specFeatures.put(
        "Bikes Allowance",
        hasAtLeastOneRecordForFields(
            feedContainer,
            GtfsTrip.FILENAME,
            List.of((Function<GtfsTrip, Boolean>) (GtfsTrip::hasBikesAllowed))));
  }

  private void loadTTSFeature(GtfsFeedContainer feedContainer) {
    specFeatures.put(
        "Text-To-Speech",
        hasAtLeastOneRecordForFields(
            feedContainer,
            GtfsStop.FILENAME,
            List.of(((Function<GtfsStop, Boolean>) GtfsStop::hasTtsStopName))));
  }

  private void loadWheelchairAccessibilityFeature(GtfsFeedContainer feedContainer) {
    specFeatures.put(
        "Wheelchair Accessibility",
        hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsTrip.FILENAME,
                List.of((Function<GtfsTrip, Boolean>) GtfsTrip::hasWheelchairAccessible))
            || hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsStop.FILENAME,
                List.of((Function<GtfsStop, Boolean>) GtfsStop::hasWheelchairBoarding)));
  }

  private void loadHeadsignsFeature(GtfsFeedContainer feedContainer) {
    specFeatures.put(
        "Headsigns",
        hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsTrip.FILENAME,
                List.of((Function<GtfsTrip, Boolean>) GtfsTrip::hasTripHeadsign))
            || hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsStopTime.FILENAME,
                List.of((Function<GtfsStopTime, Boolean>) GtfsStopTime::hasStopHeadsign)));
  }

  private void loadRouteColorsFeature(GtfsFeedContainer feedContainer) {
    specFeatures.put(
        "Route Colors",
        hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsRoute.FILENAME,
                List.of((Function<GtfsRoute, Boolean>) GtfsRoute::hasRouteColor))
            || hasAtLeastOneRecordForFields(
                feedContainer,
                GtfsRoute.FILENAME,
                List.of((Function<GtfsRoute, Boolean>) GtfsRoute::hasRouteTextColor)));
  }

  private void loadAgencyData(GtfsTableContainer<GtfsAgency> agencyTable) {
    for (GtfsAgency agency : agencyTable.getEntities()) {
      agencies.add(AgencyMetadata.from(agency));
    }
  }

  private void loadFeedInfo(GtfsTableContainer<GtfsFeedInfo> feedTable) {
    var info = feedTable.getEntities().isEmpty() ? null : feedTable.getEntities().get(0);

    feedInfo.put(FEED_INFO_PUBLISHER_NAME, info == null ? "N/A" : info.feedPublisherName());
    feedInfo.put(FEED_INFO_PUBLISHER_URL, info == null ? "N/A" : info.feedPublisherUrl());
    feedInfo.put(FEED_INFO_FEED_CONTACT_EMAIL, info == null ? "N/A" : info.feedContactEmail());
    feedInfo.put(
        FEED_INFO_FEED_LANGUAGE, info == null ? "N/A" : info.feedLang().getDisplayLanguage());
    if (feedTable.hasColumn(GtfsFeedInfo.FEED_START_DATE_FIELD_NAME)) {
      if (info != null) {
        LocalDate localDate = info.feedStartDate().getLocalDate();
        feedInfo.put(FEED_INFO_FEED_START_DATE, checkLocalDate(localDate));
      }
    }
    if (feedTable.hasColumn(GtfsFeedInfo.FEED_END_DATE_FIELD_NAME)) {
      if (info != null) {
        LocalDate localDate = info.feedEndDate().getLocalDate();
        feedInfo.put(FEED_INFO_FEED_END_DATE, checkLocalDate(localDate));
      }
    }
  }

  private String checkLocalDate(LocalDate localDate) {
    String displayDate;
    if (localDate.toString().equals(LocalDate.EPOCH.toString())) {
      displayDate = "N/A";
    } else {
      displayDate = localDate.toString();
    }
    return displayDate;
  }

  /**
   * Loads the service date range by determining the earliest start date and the latest end date for
   * all services referenced with a trip\_id in `trips.txt`. It handles three cases: 1. When only
   * `calendars.txt` is used. 2. When only `calendar\_dates.txt` is used. 3. When both
   * `calendars.txt` and `calendar\_dates.txt` are used.
   *
   * @param tripContainer the container for `trips.txt` data
   * @param calendarTable the container for `calendars.txt` data
   * @param calendarDateTable the container for `calendar\_dates.txt` data
   */
  public void loadServiceWindow(
      GtfsTableContainer<GtfsTrip> tripContainer,
      GtfsTableContainer<GtfsCalendar> calendarTable,
      GtfsTableContainer<GtfsCalendarDate> calendarDateTable) {
    List<GtfsTrip> trips = tripContainer.getEntities();

    LocalDate earliestStartDate = null;
    LocalDate latestEndDate = null;
    try {
      if ((calendarDateTable == null) && (calendarTable != null)) {
        // When only calendars.txt is used
        List<GtfsCalendar> calendars = calendarTable.getEntities();
        for (GtfsTrip trip : trips) {
          String serviceId = trip.serviceId();
          for (GtfsCalendar calendar : calendars) {
            if (calendar.serviceId().equals(serviceId)) {
              LocalDate startDate = calendar.startDate().getLocalDate();
              LocalDate endDate = calendar.endDate().getLocalDate();
              if (startDate != null || endDate != null) {
                if (startDate.toString().equals(LocalDate.EPOCH.toString())
                    || endDate.toString().equals(LocalDate.EPOCH.toString())) {
                  continue;
                }
                if (earliestStartDate == null || startDate.isBefore(earliestStartDate)) {
                  earliestStartDate = startDate;
                }
                if (latestEndDate == null || endDate.isAfter(latestEndDate)) {
                  latestEndDate = endDate;
                }
              }
            }
          }
        }
      } else if ((calendarDateTable != null) && (calendarTable == null)) {
        // When only calendar_dates.txt is used
        List<GtfsCalendarDate> calendarDates = calendarDateTable.getEntities();
        for (GtfsTrip trip : trips) {
          String serviceId = trip.serviceId();
          for (GtfsCalendarDate calendarDate : calendarDates) {
            if (calendarDate.serviceId().equals(serviceId)) {
              LocalDate date = calendarDate.date().getLocalDate();
              if (date != null && !date.toString().equals(LocalDate.EPOCH.toString())) {
                if (earliestStartDate == null || date.isBefore(earliestStartDate)) {
                  earliestStartDate = date;
                }
                if (latestEndDate == null || date.isAfter(latestEndDate)) {
                  latestEndDate = date;
                }
              }
            }
          }
        }
      } else if ((calendarTable != null) && (calendarDateTable != null)) {
        // When both calendars.txt and calendar_dates.txt are used
        Map<String, ServicePeriod> servicePeriods =
            CalendarUtil.buildServicePeriodMap(
                (GtfsCalendarTableContainer) calendarTable,
                (GtfsCalendarDateTableContainer) calendarDateTable);
        List<LocalDate> removedDates = new ArrayList<>();
        for (GtfsTrip trip : trips) {
          String serviceId = trip.serviceId();
          ServicePeriod servicePeriod = servicePeriods.get(serviceId);
          LocalDate startDate = servicePeriod.getServiceStart();
          LocalDate endDate = servicePeriod.getServiceEnd();
          if (startDate != null && endDate != null) {
            if (startDate.toString().equals(LocalDate.EPOCH.toString())
                || endDate.toString().equals(LocalDate.EPOCH.toString())) {
              continue;
            }
            if (earliestStartDate == null || startDate.isBefore(earliestStartDate)) {
              earliestStartDate = startDate;
            }
            if (latestEndDate == null || endDate.isAfter(latestEndDate)) {
              latestEndDate = endDate;
            }
          }
          removedDates.addAll(servicePeriod.getRemovedDays());
        }

        for (LocalDate date : removedDates) {
          if (date.isEqual(earliestStartDate)) {
            earliestStartDate = date.plusDays(1);
          }
          if (date.isEqual(latestEndDate)) {
            latestEndDate = date.minusDays(1);
          }
        }
      }
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Error while loading Service Window");
    } finally {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
      if ((earliestStartDate == null) && (latestEndDate == null)) {
        feedInfo.put(FEED_INFO_SERVICE_WINDOW, "N/A");
      } else if (earliestStartDate == null && latestEndDate != null) {
        feedInfo.put(FEED_INFO_SERVICE_WINDOW, latestEndDate.format(formatter));
      } else if (latestEndDate == null && earliestStartDate != null) {
        if (earliestStartDate.isAfter(latestEndDate)) {
          feedInfo.put(FEED_INFO_SERVICE_WINDOW, "N/A");
        } else {
          feedInfo.put(FEED_INFO_SERVICE_WINDOW, earliestStartDate.format(formatter));
        }
      } else {
        StringBuilder serviceWindow = new StringBuilder();
        serviceWindow.append(earliestStartDate);
        serviceWindow.append(" to ");
        serviceWindow.append(latestEndDate);
        feedInfo.put(FEED_INFO_SERVICE_WINDOW, serviceWindow.toString());
      }
      feedInfo.put(
          FEED_INFO_SERVICE_WINDOW_START,
          earliestStartDate == null ? "" : earliestStartDate.toString());
      feedInfo.put(
          FEED_INFO_SERVICE_WINDOW_END, latestEndDate == null ? "" : latestEndDate.toString());
    }
  }

  private boolean hasAtLeastOneRecordInFile(
      GtfsFeedContainer feedContainer, String featureFilename) {
    var table = feedContainer.getTableForFilename(featureFilename);
    return table.isPresent() && table.get().entityCount() > 0;
  }

  private <T extends GtfsEntity> boolean hasAtLeastOneRecordForFields(
      GtfsFeedContainer feedContainer,
      String featureFilename,
      List<Function<T, Boolean>> conditions) {
    return feedContainer
        .getTableForFilename(featureFilename)
        .map(
            table ->
                table.getEntities().stream()
                    .anyMatch( // all values need to be defined for the same entry
                        entity ->
                            conditions.stream().allMatch(condition -> condition.apply((T) entity))))
        .orElse(false);
  }

  public ArrayList<String> foundFiles() {
    var foundFiles = new ArrayList<String>();
    for (var table : tableMetaData.values()) {
      if (table.getTableStatus() != GtfsTableContainer.TableStatus.MISSING_FILE) {
        foundFiles.add(table.getFilename());
      }
    }
    return foundFiles;
  }

  public void setTableMetaData(Map<String, TableMetadata> tableMetaData) {
    this.tableMetaData = tableMetaData;
  }

  public void setFilenames(ImmutableSortedSet<String> filenames) {
    this.filenames = filenames;
  }

  public ImmutableSortedSet<String> getFilenames() {
    return filenames;
  }
}
