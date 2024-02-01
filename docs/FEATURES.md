# Discovery of component in feeds

The validator will produce the [list of components](https://docs.google.com/spreadsheets/d/1kpbKOzlHtsJIPo3B4ABYu-Nxwvqutjcdcvgpa0wZvtA) that it finds in the processed feed according to this table:

| Component                | Feature                   | How is the presence of a feature determined (minimum requirements)                                                                                                                                                                                                                                                                                                                                                                                |
|--------------------------|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Accesibility             | Text-to-speech            | One **tts_stop_name** value in [stops.txt](https://gtfs.org/schedule/reference/#stopstxt)                                                                                                                                                                                                                                                                                                                                                         |
| Accesibility             | Wheelchair accesibility   | One **wheelchair_accessible** value in [trips.txt](https://gtfs.org/schedule/reference/#tripstxt) <br>OR<br>one **wheelchair_boarding** value in [stops.txt](https://gtfs.org/schedule/reference/#stopstxt)                                                                                                                                                                                                                                       |
| Accesibility             | Route Colors              | One **color** value <br>OR<br>one text_color value in [routes.txt](https://gtfs.org/schedule/reference/#routestxt)                                                                                                                                                                                                                                                                                                                                |
| Accesibility             | Bike Allowed              | One **bikes_allowed** value in [trips.txt](https://gtfs.org/schedule/reference/#tripstxt)                                                                                                                                                                                                                                                                                                                                                         |
| Accesibility             | Translations              | One line of data in [translations.txt](https://gtfs.org/schedule/reference/#translationstxt)                                                                                                                                                                                                                                                                                                                                                      |
| Accesibility             | Headsigns                 | One **trip_headsign** in [trips.txt](https://gtfs.org/schedule/reference/#tripstxt)<br>OR<br>one stop_headsign value in [stop_times.txt](https://gtfs.org/schedule/reference/#stop_timestxt)                                                                                                                                                                                                                                                      |
| Fares                    | Fare Products             | One line of data in [fare_products.txt](https://gtfs.org/schedule/reference/#fare_productstxt)                                                                                                                                                                                                                                                                                                                                                    |
| Fares                    | Fare Media                | One line of data in [fare_media.txt](https://gtfs.org/schedule/reference/#fare_mediatxt)                                                                                                                                                                                                                                                                                                                                                          |
| Fares                    | Route-Based Fares         | One **network_id** value in [routes.txt](https://gtfs.org/schedule/reference/#routestxt)<br/>OR<br/>one line of data in [networks..txt](https://gtfs.org/schedule/reference/#networkstxt)                                                                                                                                                                                                                                                         |
| Fares                    | Time-Based Fares          | One line of data in [timeframes.txt](https://gtfs.org/schedule/reference/#timeframestxt)                                                                                                                                                                                                                                                                                                                                                          |
| Fares                    | Zone-Based Fares          | One line of data in [areas.txt](https://gtfs.org/schedule/reference/#areastxt)                                                                                                                                                                                                                                                                                                                                                                    |
| Fares                    | Transfer Fares            | One line of data in [fare_transfer_rules.txt](https://gtfs.org/schedule/reference/#fare_transfer_rulestxt)                                                                                                                                                                                                                                                                                                                                        |
| Fares                    | Fares V1                  | One line of data in [fare_attributes.txt](https://gtfs.org/schedule/reference/#fare_attributestxt)                                                                                                                                                                                                                                                                                                                                                |
| Pathways                 | Pathways (basic)*         | One line of data in [pathways.txt](https://gtfs.org/schedule/reference/#pathwaystxt)                                                                                                                                                                                                                                                                                                                                                              |
| Pathways                 | Pathways (extra)*         | One value of **max_slope**<br/>OR<br/>**min_width** <br/>OR<br/>**length** <br/>OR<br/>**stair_count** in [pathways.txt](https://gtfs.org/schedule/reference/#pathwaystxt)                                                                                                                                                                                                                                                                        |
| Pathways                 | Levels                    | One line of data in [levels.txt](https://gtfs.org/schedule/reference/#levelstxt)                                                                                                                                                                                                                                                                                                                                                                  |
| Pathways                 | In-station traversal time | One **traversal_time** value in [pathways.txt](https://gtfs.org/schedule/reference/#pathwaystxt)                                                                                                                                                                                                                                                                                                                                                  |
| Pathways                 | Pathways directions       | One **signposted_as** value<br/>AND<br/>one **reversed_signposted_as** in [pathways.txt](https://gtfs.org/schedule/reference/#pathwaystxt)                                                                                                                                                                                                                                                                                                        |
| Pathways                 | Location types            | One **location_type** value in [stops.txt](https://gtfs.org/schedule/reference/#stopstxt)                                                                                                                                                                                                                                                                                                                                                         |
| Metadata                 | Feed Information          | One line of data in [feed_info.txt](https://gtfs.org/schedule/reference/#feed_infotxt)                                                                                                                                                                                                                                                                                                                                                            |
| Metadata                 | Attributions              | One line of data in [attributions.txt](https://gtfs.org/schedule/reference/#attributionstxt)                                                                                                                                                                                                                                                                                                                                                      |
| Flexible Services        | Continuous Stops          | One **continuous_dropoff** value in [routes.txt](https://gtfs.org/schedule/reference/#routestxt)<br/>OR<br/>one **continuous_pickup** value in [routes.txt](https://gtfs.org/schedule/reference/#routestxt)<br/>OR<br/>one **continuous_dropoff** value in [stop_times.txt](https://gtfs.org/schedule/reference/#stop_timestxt)<br/>OR<br/>one **continuous_pickup** value in [stop_times.txt](https://gtfs.org/schedule/reference/#stop_timestxt) |
| Shapes                   | Shapes                    | One line of data in [shapes.txt](https://gtfs.org/schedule/reference/#shapestxt)                                                                                                                                                                                                                                                                                                                                                                  |
| Transfers                | Transfers                 | One line of data in [transfers.txt](https://gtfs.org/schedule/reference/#transferstxt)                                                                                                                                                                                                                                                                                                                                                            |
| Frequency-based Services | Frequencies               | One line of data in [frequencies.txt](https://gtfs.org/schedule/reference/#frequenciestxt)                                                                                                                                                                                                                                                                                                                                                        |
