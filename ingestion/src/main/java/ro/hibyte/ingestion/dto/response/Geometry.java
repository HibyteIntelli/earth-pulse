package ro.hibyte.ingestion.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class Geometry {
    private String type;
    private List<Double> coordinates;

    public Geometry(double longitude, double latitude) {
        this.type = "Point";
        this.coordinates = List.of(longitude, latitude);
    }
}
