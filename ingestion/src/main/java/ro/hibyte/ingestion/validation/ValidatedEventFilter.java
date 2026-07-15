package ro.hibyte.ingestion.validation;

import ro.hibyte.ingestion.dto.request.EventFilter;

public record ValidatedEventFilter(EventFilter original, BoundingBox bbox, int page, int size) {}
