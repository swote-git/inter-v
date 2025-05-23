package dev.swote.interv.domain.user.entity;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import dev.swote.interv.domain.BaseEntity;
import dev.swote.interv.domain.interview.entity.Question;
import dev.swote.interv.domain.user.VO.RegisterVO;
import dev.swote.interv.util.security.PasswordEncoder;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.Where;

@Entity
@Getter
@Setter
@Builder
@Table(name = "tb_user")
@Where(clause = "deleted_at IS NULL")
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String email;

    private String password;

    private String salt;

    private String userName;

    private String nickname;

    private String phoneNumber;

    private LocalDate birthDate;

    private String name; // 테스트용 추가

    @ManyToMany
    @JoinTable(
            name = "user_favorite_questions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private Set<Question> favoritedQuestions = new HashSet<>();

    public static User of(RegisterVO registerVO, PasswordEncoder encoder) throws NoSuchAlgorithmException {
        final String salt = encoder.getNextSalt();

        return builder()
                .email(registerVO.getEmail())
                .salt(salt)
                .password(encoder.hash(registerVO.getPassword(), salt))
                .nickname(registerVO.getNickname())
                .phoneNumber(registerVO.getPhoneNumber())
                .birthDate(registerVO.getBirthDate())
                .favoritedQuestions(new HashSet<>())
                .build();
    }
    public void setName(String name) {
        this.name = name;
    }
}