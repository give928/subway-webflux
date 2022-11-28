package nextstep.subway.map.application;

import nextstep.subway.common.annotation.Loggable;
import nextstep.subway.line.application.LineService;
import nextstep.subway.map.dto.PathResponse;
import nextstep.subway.map.dto.PathResponseAssembler;
import nextstep.subway.station.application.StationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

@Service
@Transactional(readOnly = true, transactionManager = "readTransactionManager")
public class MapService {
    private final LineService lineService;
    private final StationService stationService;
    private final PathService pathService;

    public MapService(LineService lineService, StationService stationService, PathService pathService) {
        this.lineService = lineService;
        this.stationService = stationService;
        this.pathService = pathService;
    }

    // @formatter:off
    @Loggable(json = true)
    public Mono<PathResponse> findPath(Long source, Long target) {
        return lineService.findLines()
                .collectList()
                .publishOn(Schedulers.boundedElastic())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(lines -> stationService.findById(source)
                        .flatMap(sourceStation -> Mono.just(Tuples.of(lines, sourceStation))))
                .flatMap(tuple -> stationService.findById(target)
                        .flatMap(targetStation -> pathService.findPath(tuple.getT1(), tuple.getT2(), targetStation)))
                .map(PathResponseAssembler::assemble);
    }
    // @formatter:on
}
