package com.jaeuk.job_ai.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 통합 테스트 베이스 클래스.
 *
 * <p>PostgreSQL 컨테이너를 JVM 전체 생명주기에 걸쳐 단 하나만 기동한다
 * ({@code static} 필드 + Testcontainers Singleton 패턴).
 * {@link ServiceConnection} 이 datasource URL/user/password 를 자동 연결한다.</p>
 *
 * <p>Redis 는 테스트 프로파일({@code test})의 {@code application-test.yml} 에서
 * {@code EmbeddedRedisConfig} 또는 Mock 으로 대체한다.</p>
 *
 * <p>OpenAI / pgvector 등 외부 의존성은 {@code @MockitoBean} 으로 대체하여
 * CI 환경에서 API 키 없이 실행 가능하게 한다.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class IntegrationTestBase {

    /**
     * pgvector 확장이 내장된 이미지를 사용한다.
     * Spring AI VectorStore 가 {@code initialize-schema: true} 로 스키마를 자동 생성하므로
     * 별도 init SQL 은 필요 없다.
     */
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("medical_rag_test")
                    .withUsername("test")
                    .withPassword("test");
}
