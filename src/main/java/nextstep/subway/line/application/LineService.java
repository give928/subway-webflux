package nextstep.subway.line.application;

import lombok.RequiredArgsConstructor;
import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.LineRepository;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.line.dto.LinesResponse;
import nextstep.subway.line.dto.SectionRequest;
import nextstep.subway.station.application.StationService;
import nextstep.subway.station.domain.Station;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LineService {
    private final LineRepository lineRepository;
    private final StationService stationService;

    @CacheEvict(value = {"lines", "line-responses", "find-path"}, allEntries = true)
    @Transactional
    public LineResponse saveLine(LineRequest request) {
        Station upStation = stationService.findById(request.getUpStationId());
        Station downStation = stationService.findById(request.getDownStationId());
        Line persistLine = lineRepository.save(
                new Line(request.getName(), request.getColor(), upStation, downStation, request.getDistance()));
        return LineResponse.of(persistLine);
    }

    @Cacheable("line-responses")
    public List<LinesResponse> findLineResponses() {
        List<Line> persistLines = lineRepository.findAll();
        return persistLines.stream().map(LinesResponse::of).collect(Collectors.toList());
    }

    @Cacheable("lines")
    public List<Line> findLines() {
        return lineRepository.findAll();
    }

    @Cacheable(value = "line", key = "#id")
    public Line findLineById(Long id) {
        return lineRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    @Cacheable(value = "line-response", key = "#id")
    public LineResponse findLineResponseById(Long id) {
        Line persistLine = findLineById(id);
        return LineResponse.of(persistLine);
    }

    @Caching(put = {@CachePut(value = "line", key = "#id")},
            evict = {@CacheEvict(value = {"lines", "line-responses", "find-path"}, allEntries = true),
                    @CacheEvict(value = "line-response", key = "#id")})
    @Transactional
    public Line updateLine(Long id, LineRequest lineUpdateRequest) {
        Line persistLine = lineRepository.findById(id).orElseThrow(RuntimeException::new);
        persistLine.update(new Line(lineUpdateRequest.getName(), lineUpdateRequest.getColor()));
        return persistLine;
    }

    @Caching(evict = {@CacheEvict(value = {"lines", "line-responses", "find-path"}, allEntries = true),
            @CacheEvict(value = {"line", "line-response"}, key = "#id")})
    @Transactional
    public void deleteLineById(Long id) {
        lineRepository.deleteById(id);
    }

    @Caching(evict = {@CacheEvict(value = {"lines", "line-responses", "find-path"}, allEntries = true),
            @CacheEvict(value = {"line", "line-response"}, key = "#lineId")})
    @Transactional
    public void addLineStation(Long lineId, SectionRequest request) {
        Line line = findLineById(lineId);
        Station upStation = stationService.findStationById(request.getUpStationId());
        Station downStation = stationService.findStationById(request.getDownStationId());
        line.addLineSection(upStation, downStation, request.getDistance());
    }

    @Caching(evict = {@CacheEvict(value = {"lines", "line-responses", "find-path"}, allEntries = true),
            @CacheEvict(value = {"line", "line-response"}, key = "#lineId")})
    @Transactional
    public void removeLineStation(Long lineId, Long stationId) {
        Line line = findLineById(lineId);
        line.removeStation(stationId);
    }

}
