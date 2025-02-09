package com.dnd.ground.domain.user.repository;

import com.dnd.ground.domain.exerciseRecord.ExerciseRecord;
import com.dnd.ground.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * @description 유저 리포지토리 인터페이스
 * @author  박세헌, 박찬호
 * @since   2022-08-01
 * @updated 1. 리프레시 토큰을 사용해 회원 조회하기
 *  *            - 2022-09-23 박찬호
 */

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNickname(String nickname);

    @Query("select u from User u join u.exerciseRecords e where e = :exerciseRecord")
    Optional<User> findByExerciseRecord(ExerciseRecord exerciseRecord);

    Optional<User> findByKakaoId(Long id);

    Optional<User> findByRefreshToken(String token);

    Boolean existsByKakaoId(Long id);

    void deleteByNickname(String nickname);

}
