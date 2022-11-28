package nextstep.subway.favorite.application;

import nextstep.subway.auth.domain.LoginMember;
import nextstep.subway.favorite.domain.Favorite;
import nextstep.subway.favorite.domain.FavoriteRepository;
import nextstep.subway.favorite.domain.HasNotPermissionException;
import nextstep.subway.favorite.dto.FavoriteRequest;
import nextstep.subway.favorite.dto.FavoriteResponse;
import nextstep.subway.station.application.StationService;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.dto.StationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true, transactionManager = "readTransactionManager")
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final StationService stationService;

    public FavoriteService(FavoriteRepository favoriteRepository, StationService stationService) {
        this.favoriteRepository = favoriteRepository;
        this.stationService = stationService;
    }

    // @formatter:off
    @Transactional
    public Mono<Favorite> createFavorite(LoginMember loginMember, FavoriteRequest request) {
        return favoriteRepository.save(Favorite.builder()
                                               .memberId(loginMember.getId())
                                               .sourceStationId(request.getSource())
                                               .targetStationId(request.getTarget())
                                               .build());
    }
    // @formatter:on

    // @formatter:off
    public Flux<FavoriteResponse> findFavorites(LoginMember loginMember) {
        return favoriteRepository.findByMemberId(loginMember.getId())
                .collectList()
                .flatMap(favorites -> extractStations(favorites).flatMap(map -> Mono.just(Tuples.of(favorites, map))))
                .map(tuple -> mapFavoriteStream(tuple.getT1(), tuple.getT2()))
                .flatMapMany(Flux::fromStream);
    }
    // @formatter:on

    private Mono<Map<Long, Station>> extractStations(List<Favorite> favorites) {
        return stationService.findAllById(extractStationIds(favorites))
                .collectMap(Station::getId, Function.identity());
    }

    private Set<Long> extractStationIds(List<Favorite> favorites) {
        return favorites.stream()
                .flatMap(favorite -> Stream.of(favorite.getSourceStationId(), favorite.getTargetStationId()))
                .collect(Collectors.toSet());
    }

    // @formatter:off
    private static Stream<FavoriteResponse> mapFavoriteStream(List<Favorite> favorites, Map<Long, Station> stations) {
        return favorites.stream()
                .map(favorite -> FavoriteResponse.builder()
                        .id(favorite.getId())
                        .source(StationResponse.of(stations.get(favorite.getSourceStationId())))
                        .target(StationResponse.of(stations.get(favorite.getTargetStationId())))
                        .build());
    }
    // @formatter:on

    // @formatter:off
    @Transactional
    public Mono<Void> deleteFavorite(LoginMember loginMember, Long id) {
        return favoriteRepository.findById(id)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException())))
                .map(favorite -> {
                    if (!favorite.isCreatedBy(loginMember.getId())) {
                        throw new HasNotPermissionException(loginMember.getId() + "는 삭제할 권한이 없습니다.");
                    }
                    return favorite;
                })
                .onErrorResume(throwable -> Mono.defer(() -> Mono.error(throwable)))
                .flatMap(favoriteRepository::delete);
    }
    // @formatter:on
}
