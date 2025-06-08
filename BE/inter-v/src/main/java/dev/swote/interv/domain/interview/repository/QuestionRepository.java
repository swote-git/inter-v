package dev.swote.interv.domain.interview.repository;

import dev.swote.interv.domain.interview.entity.InterviewSession;
import dev.swote.interv.domain.interview.entity.Question;
import dev.swote.interv.domain.interview.entity.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findByInterviewSessionOrderBySequenceAsc(InterviewSession interviewSession);

    @Query("SELECT q FROM Question q WHERE " +
            "(:category IS NULL OR q.category = :category) AND " +
            "(:difficultyLevel IS NULL OR q.difficultyLevel = :difficultyLevel) AND " +
            "(:type IS NULL OR q.type = :type) AND " +
            "(:keyword IS NULL OR q.content LIKE %:keyword%)")
    Page<Question> findQuestionsByFilters(
            @Param("category") String category,
            @Param("difficultyLevel") Integer difficultyLevel,
            @Param("type") QuestionType type,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // Remove this method - it's causing the error
    // List<Question> findRandomQuestionsByTypeAndCount(QuestionType type, int count);

    @Query(value = "SELECT * FROM tb_question q WHERE q.type = :type ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Question> findRandomQuestions(@Param("type") String type, @Param("count") int count);

    @Query("SELECT MAX(q.sequence) FROM Question q WHERE q.interviewSession.id = :interviewId")
    Integer findMaxOrderByInterviewId(@Param("interviewId") Integer interviewId);

    /**
     * 면접 세션의 최대 sequence 값 조회
     */
    @Query("SELECT MAX(q.sequence) FROM Question q WHERE q.interviewSession.id = :interviewId")
    Integer findMaxSequenceByInterviewId(@Param("interviewId") Integer interviewId);

    /**
     * 면접 세션의 질문들을 sequence 순으로 조회
     */
    @Query("SELECT q FROM Question q WHERE q.interviewSession.id = :interviewId ORDER BY q.sequence ASC")
    List<Question> findByInterviewSessionIdOrderBySequence(@Param("interviewId") Integer interviewId);

    /**
     * 랜덤 질문 조회 (타입별)
     */
    @Query("SELECT q FROM Question q WHERE q.type = :type ORDER BY RAND()")
    List<Question> findRandomQuestions(@Param("type") String type, Pageable pageable);

    /**
     * AI로 생성된 질문들 조회
     */
    @Query("SELECT q FROM Question q WHERE q.interviewSession.id = :interviewId AND q.category LIKE '%AI%' ORDER BY q.sequence ASC")
    List<Question> findAIGeneratedQuestions(@Param("interviewId") Integer interviewId);

    /**
     * 면접 세션의 질문 개수 조회
     */
    @Query("SELECT COUNT(q) FROM Question q WHERE q.interviewSession.id = :interviewId")
    Long countByInterviewSessionId(@Param("interviewId") Integer interviewId);
}