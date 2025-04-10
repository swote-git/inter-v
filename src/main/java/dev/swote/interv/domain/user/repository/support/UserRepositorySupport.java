package dev.swote.interv.domain.user.repository.support;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRepositorySupport {

    private final JPAQueryFactory jpaQueryFactory;


}