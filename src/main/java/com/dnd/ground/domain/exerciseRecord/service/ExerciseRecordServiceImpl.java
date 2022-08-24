package com.dnd.ground.domain.exerciseRecord.service;

import com.dnd.ground.domain.exerciseRecord.ExerciseRecord;
import com.dnd.ground.domain.exerciseRecord.Repository.ExerciseRecordRepository;
import com.dnd.ground.domain.exerciseRecord.dto.EndRequestDto;
import com.dnd.ground.domain.exerciseRecord.dto.StartResponseDto;
import com.dnd.ground.domain.friend.service.FriendService;
import com.dnd.ground.domain.matrix.Matrix;
import com.dnd.ground.domain.matrix.matrixRepository.MatrixRepository;
import com.dnd.ground.domain.user.User;
import com.dnd.ground.domain.user.dto.RankResponseDto;
import com.dnd.ground.domain.user.dto.UserResponseDto;
import com.dnd.ground.domain.user.repository.UserRepository;
import com.dnd.ground.global.exception.CNotFoundException;
import com.dnd.ground.global.exception.CommonErrorCode;
import lombok.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Tuple;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @description 운동 기록 서비스 클래스
 * @author  박세헌
 * @since   2022-08-01
 * @updated 2022-08-24 / 1. orElseThrow() 예외 처리 - 박찬호
 *                       2. 랭킹 동점 로직, 유저 맨위 - 박세헌
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseRecordServiceImpl implements ExerciseRecordService {

    private final ExerciseRecordRepository exerciseRecordRepository;
    private final UserRepository userRepository;
    private final MatrixRepository matrixRepository;
    private final FriendService friendService;

    @Transactional
    public void delete(Long exerciseRecordId) {
        exerciseRecordRepository.deleteById(exerciseRecordId);
    }

    // 기록 시작
    // 운동기록 id, 일주일 누적 영역 반환
    @Transactional
    public StartResponseDto recordStart(String nickname) {
        User user = userRepository.findByNickname(nickname).orElseThrow(
                () -> new CNotFoundException(CommonErrorCode.NOT_FOUND_USER));

        List<ExerciseRecord> recordOfThisWeek = exerciseRecordRepository.findRecordOfThisWeek(user.getId());
        if (recordOfThisWeek.isEmpty()) {
            return new StartResponseDto(0L);
        }

        return new StartResponseDto((long) matrixRepository.findMatrixSetByRecords(recordOfThisWeek).size());
    }

    // 기록 끝
    @Transactional
    public ResponseEntity<?> recordEnd(EndRequestDto endRequestDto) {
        // 유저 찾아서 운동 기록 생성

        User user = userRepository.findByNickname(endRequestDto.getNickname()).orElseThrow(
                () -> new CNotFoundException(CommonErrorCode.NOT_FOUND_USER));
        ExerciseRecord exerciseRecord = new ExerciseRecord(user);

        // 정보 update(ended, 거리, 걸음수, 운동시간, 상세 기록, 시작 시간, 끝 시간)
        exerciseRecord.updateInfo(endRequestDto.getDistance(), endRequestDto.getStepCount(),
                endRequestDto.getExerciseTime(), endRequestDto.getMessage(), endRequestDto.getStarted(), endRequestDto.getEnded());

        //영역 저장
        ArrayList<ArrayList<Double>> matrices = endRequestDto.getMatrices();
        for (int i = 0; i < matrices.size(); i++) {
            exerciseRecord.addMatrix(new Matrix(matrices.get(i).get(0), matrices.get(i).get(1)));
        }

        //회원 마지막 위치 최신화
        ArrayList<Double> lastPosition = matrices.get(matrices.size() - 1);
        exerciseRecord.getUser().updatePosition(lastPosition.get(0), lastPosition.get(1));

        exerciseRecordRepository.save(exerciseRecord);
        return new ResponseEntity("성공", HttpStatus.CREATED);
    }

    // 랭킹 조회(누적 걸음 수 기준)  (추후 파라미터 Requestdto로 교체 예정)
    public RankResponseDto.Step stepRanking(String nickname, LocalDateTime start, LocalDateTime end) {
        User user = userRepository.findByNickname(nickname).orElseThrow(
                () -> new CNotFoundException(CommonErrorCode.NOT_FOUND_USER));

        List<User> userAndFriends = friendService.getFriends(user);  // 친구들 조회
        userAndFriends.add(0, user);  // 유저 추가
        List<UserResponseDto.Ranking> stepRankings = new ArrayList<>(); // [랭킹, 닉네임, 걸음 수]

        // [Tuple(닉네임, 걸음 수)] 걸음 수 기준 내림차순 정렬
        List<Tuple> stepCount = exerciseRecordRepository.findStepCount(userAndFriends, start, end);

        int count = 0;
        int rank = 1;

        UserResponseDto.Ranking userRanking = null;

        Long matrixNumber = (Long) stepCount.get(0).get(1);  // 맨 처음 user의 걸음 수
        for (Tuple info : stepCount) {
            // 전 유저와 걸음 수가 같다면 랭크 유지
            if (Objects.equals((Long) info.get(1), matrixNumber)) {

                // 유저 찾았으면 저장해둠
                if (Objects.equals((String) info.get(0), user.getNickname())) {
                    userRanking = new UserResponseDto.Ranking(rank, (String) info.get(0),
                            (Long) info.get(1));
                }

                stepRankings.add(new UserResponseDto.Ranking(rank, (String) info.get(0),
                        (Long) info.get(1)));
                count += 1;
                continue;
            }

            // 전 유저보다 걸음수가 작다면 앞에 있는 사람수 만큼이 자신 랭킹
            count += 1;
            rank = count;

            // 유저 찾았으면 저장해둠
            if (Objects.equals((String) info.get(0), user.getNickname())) {
                userRanking = new UserResponseDto.Ranking(rank, (String) info.get(0),
                        (Long) info.get(1));
            }
            stepRankings.add(new UserResponseDto.Ranking(rank, (String) info.get(0),
                    (Long) info.get(1)));
            matrixNumber = (Long) info.get(1);  // 걸음 수 update!
        }
        // 맨 앞에 유저 추가
        stepRankings.add(0, userRanking);
        return new RankResponseDto.Step(stepRankings);
    }
}
