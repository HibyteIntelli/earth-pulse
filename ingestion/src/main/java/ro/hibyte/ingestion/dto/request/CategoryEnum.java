package ro.hibyte.ingestion.dto.request;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum CategoryEnum {
    DROUGHT("drought", "Drought"),
    DUST_HAZE("dustHaze", "Dust and Haze"),
    EARTHQUAKES("earthquakes", "Earthquakes"),
    FLOODS("floods", "Floods"),
    LANDSLIDES("landslides", "Landslides"),
    MANMADE("manmade", "Manmade"),
    SEA_LAKE_ICE("seaLakeIce", "Sea and Lake Ice"),
    SEVERE_STORMS("severeStorms", "Severe Storms"),
    SNOW("snow", "Snow"),
    TEMP_EXTREMES("tempExtremes", "Temperature Extremes"),
    VOLCANOES("volcanoes", "Volcanoes"),
    WATER_COLOR("waterColor", "Water Color"),
    WILDFIRES("wildfires", "Wildfires");

    private final String value;
    @Getter
    private final String title;

    CategoryEnum(String value, String title) {
        this.value = value;
        this.title = title;
    }

    @JsonValue
    public String getValue() { return value; }

    public static Optional<CategoryEnum> fromValue(String value) {
        return Arrays.stream(values())
                .filter(c -> c.value.equals(value))
                .findFirst();
    }
}
