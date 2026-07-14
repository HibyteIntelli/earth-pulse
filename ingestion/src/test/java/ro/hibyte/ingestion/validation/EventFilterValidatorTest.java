package ro.hibyte.ingestion.validation;

import org.junit.jupiter.api.Test;
import ro.hibyte.ingestion.dto.request.EventFilter;
import ro.hibyte.ingestion.dto.request.StatusEnum;
import ro.hibyte.ingestion.exception.ErrorCode;
import ro.hibyte.ingestion.exception.InvalidFilterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventFilterValidatorTest {

    private final EventFilterValidator validator = new EventFilterValidator();

    @Test
    void appliesDefaultsWhenFilterIsNull() {
        ValidatedEventFilter validated = validator.validate(null);

        assertThat(validated.page()).isEqualTo(0);
        assertThat(validated.size()).isEqualTo(100);
        assertThat(validated.bbox()).isNull();
    }

    @Test
    void parsesValidBbox() {
        EventFilter filter = new EventFilter();
        filter.setBbox("10,40,20,30"); // minLon, maxLat, maxLon, minLat

        BoundingBox bbox = validator.validate(filter).bbox();

        assertThat(bbox.minLon()).isEqualTo(10);
        assertThat(bbox.maxLat()).isEqualTo(40);
        assertThat(bbox.maxLon()).isEqualTo(20);
        assertThat(bbox.minLat()).isEqualTo(30);
    }

    @Test
    void rejectsBboxWithWrongPartCount() {
        EventFilter filter = new EventFilter();
        filter.setBbox("10,20,30");

        InvalidFilterException ex = assertThrows(InvalidFilterException.class, () -> validator.validate(filter));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_BBOX);
    }

    @Test
    void rejectsBboxWithNonNumericValues() {
        EventFilter filter = new EventFilter();
        filter.setBbox("a,b,c,d");

        InvalidFilterException ex = assertThrows(InvalidFilterException.class, () -> validator.validate(filter));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_BBOX);
    }

    @Test
    void rejectsBboxWithNonFiniteValues() {
        EventFilter filter = new EventFilter();
        filter.setBbox("Infinity,40,20,30");

        InvalidFilterException ex = assertThrows(InvalidFilterException.class, () -> validator.validate(filter));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_BBOX);
    }

    @Test
    void rejectsBboxWhereMinExceedsMax() {
        EventFilter filter = new EventFilter();
        filter.setBbox("20,30,10,40"); // minLon(20) > maxLon(10)

        InvalidFilterException ex = assertThrows(InvalidFilterException.class, () -> validator.validate(filter));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_BBOX);
    }

    @Test
    void appliesDefaultSizeAndPageWhenMissing() {
        EventFilter filter = new EventFilter();

        ValidatedEventFilter validated = validator.validate(filter);

        assertThat(validated.size()).isEqualTo(100);
        assertThat(validated.page()).isEqualTo(0);
    }

    @Test
    void rejectsSizeOutOfRange() {
        EventFilter filter = new EventFilter();
        filter.setSize(501);

        InvalidFilterException ex = assertThrows(InvalidFilterException.class, () -> validator.validate(filter));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.SIZE_OUT_OF_RANGE);
    }

    @Test
    void rejectsNegativePage() {
        EventFilter filter = new EventFilter();
        filter.setPage(-1);

        InvalidFilterException ex = assertThrows(InvalidFilterException.class, () -> validator.validate(filter));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.PAGE_OUT_OF_RANGE);
    }

    @Test
    void preservesOriginalFilterForUnvalidatedFields() {
        EventFilter filter = new EventFilter();
        filter.setStatus(StatusEnum.CLOSED);

        ValidatedEventFilter validated = validator.validate(filter);

        assertThat(validated.original().getStatus()).isEqualTo(StatusEnum.CLOSED);
    }
}
