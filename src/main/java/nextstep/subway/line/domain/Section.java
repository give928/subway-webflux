package nextstep.subway.line.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nextstep.subway.station.domain.Station;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@Getter
public class Section implements Serializable {
    private static final long serialVersionUID = -6362608601955321872L;

    @Id
    private Long id;

    private Long lineId;

    private Long upStationId;

    private Long downStationId;

    private int distance;

    @JsonBackReference
    @Transient
    private Line line;

    @Transient
    private Station upStation;

    @Transient
    private Station downStation;

    @Builder
    private Section(Long id, Line line, Station upStation, Station downStation, int distance) {
        this.id = id;
        this.line = line;
        this.lineId = line.getId();
        this.upStation = upStation;
        this.upStationId = upStation.getId();
        this.downStation = downStation;
        this.downStationId = downStation.getId();
        this.distance = distance;
    }

    Section initLine(Line line) {
        this.line = line;
        this.lineId = line.getId();
        return this;
    }

    Section initStation(Station upStation, Station downStation) {
        this.upStation = upStation;
        this.upStationId = upStation.getId();
        this.downStation = downStation;
        this.downStationId = downStation.getId();
        return this;
    }

    Boolean equalUpStation(Long stationId) {
        return upStationId.equals(stationId);
    }

    Boolean equalUpStation(Station station) {
        return equalUpStation(station.getId());
    }

    Boolean equalDownStation(Long stationId) {
        return downStationId.equals(stationId);
    }

    Boolean equalDownStation(Station station) {
        return equalDownStation(station.getId());
    }

    void updateUpStation(Station station, int newDistance) {
        if (this.distance < newDistance) {
            throw new IllegalArgumentException("역과 역 사이의 거리보다 좁은 거리를 입력해주세요");
        }
        this.upStationId = station.getId();
        this.distance -= newDistance;
    }

    void updateDownStation(Station station, int newDistance) {
        if (this.distance < newDistance) {
            throw new IllegalArgumentException("역과 역 사이의 거리보다 좁은 거리를 입력해주세요");
        }
        this.downStationId = station.getId();
        this.distance -= newDistance;
    }

    boolean existDownStation() {
        return this.downStationId != null;
    }

    boolean existUpStation() {
        return this.upStationId != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Section section = (Section) o;
        return getDistance() == section.getDistance() && Objects.equals(getId(),
                                                                        section.getId()) && Objects.equals(
                getLineId(), section.getLineId()) && Objects.equals(getUpStationId(),
                                                                    section.getUpStationId()) && Objects.equals(
                getDownStationId(), section.getDownStationId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getLineId(), getUpStationId(), getDownStationId(), getDistance());
    }
}
