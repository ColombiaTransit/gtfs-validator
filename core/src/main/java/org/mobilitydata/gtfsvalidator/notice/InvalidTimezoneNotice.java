/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mobilitydata.gtfsvalidator.notice;

/**
 * A field cannot be parsed as a timezone.
 *
 * <p>Timezones are defined at <a href="https://www.iana.org/time-zones">www.iana.org</a>. Timezone
 * names never contain the space character but may contain an underscore. Refer to <a
 * href="http://en.wikipedia.org/wiki/List_of_tz_zones">Wikipedia</a> for a list of valid values.
 *
 * <p>Example: {@code Asia/Tokyo}, {@code America/Los_Angeles} or {@code Africa/Cairo}.
 *
 * <p>Severity: {@code SeverityLevel.ERROR}
 */
public class InvalidTimezoneNotice extends ValidationNotice {
  private final String filename;
  private final int csvRowNumber;
  private final String fieldName;
  private final String fieldValue;

  public InvalidTimezoneNotice(
      String filename, int csvRowNumber, String fieldName, String fieldValue) {
    super(SeverityLevel.ERROR);
    this.filename = filename;
    this.csvRowNumber = csvRowNumber;
    this.fieldName = fieldName;
    this.fieldValue = fieldValue;
  }
}
