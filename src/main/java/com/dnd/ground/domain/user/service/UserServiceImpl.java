package com.dnd.ground.domain.user.service;

import com.dnd.ground.domain.challenge.Challenge;
import com.dnd.ground.domain.challenge.repository.ChallengeRepository;
import com.dnd.ground.domain.challenge.repository.UserChallengeRepository;
import com.dnd.ground.domain.exerciseRecord.ExerciseRecord;
import com.dnd.ground.domain.exerciseRecord.Repository.ExerciseRecordRepository;
import com.dnd.ground.domain.exerciseRecord.service.ExerciseRecordService;
import com.dnd.ground.domain.friend.service.FriendService;
import com.dnd.ground.domain.matrix.Matrix;
import com.dnd.ground.domain.matrix.dto.MatrixSetDto;
import com.dnd.ground.domain.user.User;
import com.dnd.ground.domain.user.dto.HomeResponseDto;
import com.dnd.ground.domain.user.dto.RankResponseDto;
import com.dnd.ground.domain.user.dto.UserResponseDto;
import com.dnd.ground.domain.user.repository.UserRepository;
import lombok.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @description 유저 서비스 클래스
 * @author  박세헌, 박찬호
 * @since   2022-08-01
 * @updated 영역의 수, 칸의 수 기준 랭킹 조회
 *          - 2022.08.09 박세헌
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final FriendService friendService;
    private final ExerciseRecordService exerciseRecordService;

    @Transactional
    public User save(User user){
        return userRepository.save(user);
    }

    public HomeResponseDto showHome(String nickname){
        User user = userRepository.findByNickName(nickname).orElseThrow();  // 예외 처리

        /*유저의 matrix 와 정보 (userMatrix)*/
        Set<MatrixSetDto> userShowMatrices = new HashSet<>();
        UserResponseDto.UserMatrix userMatrix = new UserResponseDto.UserMatrix(nickname, userShowMatrices);

        List<ExerciseRecord> userRecordOfThisWeek = exerciseRecordRepository.findRecordOfThisWeek(user.getId());

        if (!userRecordOfThisWeek.isEmpty()){
            List<List<Matrix>> userMatrices = userRecordOfThisWeek.stream()
                    .map(ExerciseRecord::getMatrices)
                    .collect(Collectors.toList());

            userMatrices.forEach(ms -> ms.forEach(m ->
                    userShowMatrices.add(
                            MatrixSetDto.builder()
                            .latitude(m.getLatitude())
                            .longitude(m.getLongitude())
                            .build())
                    )
            );

            userMatrix = new UserResponseDto.UserMatrix(user.getNickName(), userShowMatrices);
        }

        /*----------*/
        //진행 중인 챌린지 목록 조회 List<UserChallenge>
        List<Challenge> challenges = challengeRepository.findChallenge(user);

        //챌린지를 함께하지 않는 친구 목록
        List<User> friendsNotChallenge = friendService.getFriends(user);

        //나랑 챌린지를 함께 하는 사람들(친구+친구X 둘 다)
        Set<User> friendsWithChallenge = new HashSet<>();

        for (Challenge challenge : challenges) {
            List<User> challengeUsers = userChallengeRepository.findChallengeUsers(challenge);
            //챌린지를 함께하고 있는 사람들 조회
            for (User cu : challengeUsers) {
                friendsWithChallenge.add(cu);
                friendsNotChallenge.remove(cu);
            }
        }
        friendsWithChallenge.remove(user);
        /*----------*/

        /*챌린지를 안하는 친구들의 matrix 와 정보 (friendMatrices)*/
        Map<String, Set<MatrixSetDto>> friendHashMap= new HashMap<>();

        friendsNotChallenge.forEach(nf -> exerciseRecordRepository.findRecordOfThisWeek(nf.getId())
                .forEach(e -> friendHashMap.put(nf.getNickName(),
                        e.getMatrices()
                                .stream().map(m -> MatrixSetDto.builder()
                                        .latitude(m.getLatitude())
                                        .longitude(m.getLongitude())
                                        .build()
                        )
                        .collect(Collectors.toSet()))));

        List<UserResponseDto.FriendMatrix> friendMatrices = new ArrayList<>();
        for (String s : friendHashMap.keySet()) {
            friendMatrices.add(new UserResponseDto.FriendMatrix(s, friendHashMap.get(s)));
        }

        /*챌린지를 하는 사람들의 matrix 와 정보 (challengeMatrices)*/
        List<UserResponseDto.ChallengeMatrix> challengeMatrices = new ArrayList<>();

        for (User friend : friendsWithChallenge) {
            Set<MatrixSetDto> showMatrices = new HashSet<>();
            Integer challengeNumber = challengeRepository.findCountChallenge(user, friend); // 구현 완!
            String challengeColor = challengeRepository.findChallengesWithFriend(user, friend).get(0).getColor();; // 구현 완!
            List<ExerciseRecord> recordOfThisWeek = exerciseRecordRepository.findRecordOfThisWeek(friend.getId());
            recordOfThisWeek.forEach(e ->
                    e.getMatrices()
                            .forEach(m -> showMatrices.add(MatrixSetDto.builder()
                                    .latitude(m.getLatitude())
                                    .longitude(m.getLongitude())
                                    .build())
                            )
            );
            challengeMatrices.add(new UserResponseDto.ChallengeMatrix(friend.getNickName(), challengeNumber, challengeColor, showMatrices));
        }

        return HomeResponseDto.builder()
                .userMatrices(userMatrix)
                .friendMatrices(friendMatrices)
                .challengeMatrices(challengeMatrices)
                .build();
    }

    // 랭킹 조회(누적 칸의 수 기준)
    public RankResponseDto.matrixRankingResponseDto matrixRanking(String nickname){
        User user = userRepository.findByNickName(nickname).orElseThrow();
        List<User> friends = friendService.getFriends(user); // 친구들 조회
        List<UserResponseDto.matrixRanking> matrixRankings = new ArrayList<>(); // [닉네임, 칸의 수]

        // 유저의 닉네임과 (이번주)칸의 수 대입
        matrixRankings.add(new UserResponseDto.matrixRanking(user.getNickName(),
                exerciseRecordService.findMatrixNumber(exerciseRecordRepository.findRecordOfThisWeek(user.getId()))));

        // 친구들의 닉네임과 (이번주)칸의 수 대입
        friends.forEach(f -> matrixRankings.add(new UserResponseDto.matrixRanking(f.getNickName(),
                exerciseRecordService.findMatrixNumber(exerciseRecordRepository.findRecordOfThisWeek(f.getId())))));

        // 칸의 수를 기준으로 내림차순 정렬
        matrixRankings.sort(new Comparator<UserResponseDto.matrixRanking>() {
            @Override
            public int compare(UserResponseDto.matrixRanking o1, UserResponseDto.matrixRanking o2) {
                return o2.getMatrixNumber().compareTo(o1.getMatrixNumber());
            }
        });

        return new RankResponseDto.matrixRankingResponseDto(matrixRankings);
    }

    // 랭킹 조회(누적 영역의 수 기준)
    public RankResponseDto.areaRankingResponseDto areaRanking(String nickname){
        User user = userRepository.findByNickName(nickname).orElseThrow();
        List<User> friends = friendService.getFriends(user);  // 친구들 조회
        List<UserResponseDto.areaRanking> areaRankings = new ArrayList<>();  // [닉네임, 영역의 수]

        // 유저의 닉네임과 (이번주)영역의 수 대입
        areaRankings.add(new UserResponseDto.areaRanking(user.getNickName(),
                exerciseRecordService.findAreaNumber(exerciseRecordRepository.findRecordOfThisWeek(user.getId()))));

        // 친구들의 닉네임과 (이번주)영역의 수 대입
        friends.forEach(f -> areaRankings.add(new UserResponseDto.areaRanking(f.getNickName(),
                exerciseRecordService.findAreaNumber(exerciseRecordRepository.findRecordOfThisWeek(f.getId())))));

        // 영역의 수를 기준으로 내림차순 정렬
        areaRankings.sort(new Comparator<UserResponseDto.areaRanking>() {
            @Override
            public int compare(UserResponseDto.areaRanking o1, UserResponseDto.areaRanking o2) {
                return o2.getAreaNumber().compareTo(o1.getAreaNumber());
            }
        });

        return new RankResponseDto.areaRankingResponseDto(areaRankings);
    }
}