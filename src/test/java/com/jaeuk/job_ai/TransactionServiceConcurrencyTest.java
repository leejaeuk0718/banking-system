package com.jaeuk.job_ai;

import com.jaeuk.job_ai.dto.TransactionDto.TransferRequest;
import com.jaeuk.job_ai.entity.User;
import com.jaeuk.job_ai.enums.AccountType;
import com.jaeuk.job_ai.enums.UserRole;
import com.jaeuk.job_ai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Transactional
class TransactionServiceConcurrencyTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder encoder;

    private User testUser;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        // 테스트 유저 생성
        testUser = userRepository.save(User.builder()
                .name("테스트")
                .email("concurrent@test.com")
                .password(encoder.encode("Test1234!"))
                .phone("01099999999")
                .birthDate("19990101")
                .role(UserRole.USER)
                .build());

        // 출금 계좌 생성 (잔액 100,000원)
        fromAccount = accountRepository.save(Account.builder()
                .user(testUser)
                .accountNumber("032-999999-000001")
                .accountHolder("테스트")
                .bankType(BankType.BNK_BUSAN)
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.valueOf(100000))
                .version(0L)
                .build());

        // 입금 계좌 생성
        toAccount = accountRepository.save(Account.builder()
                .user(testUser)
                .accountNumber("032-999999-000002")
                .accountHolder("테스트")
                .bankType(BankType.BNK_BUSAN)
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .version(0L)
                .build());
    }

    @Test
    @DisplayName("낙관적 락 없으면 데이터 정합성 깨진다 - 낙관적 락의 필요성 증명")
    void withoutOptimisticLockTest() throws InterruptedException {
        // given
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        TransferRequest request = new TransferRequest(
                "032-999999-000001",
                "032-999999-000002",
                BigDecimal.valueOf(10000),
                "동시 이체 테스트"
        );

        // when - 5개 스레드 동시 이체 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    transactionService.transfer(testUser, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("충돌 감지: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Account finalAccount = accountRepository
                .findByAccountNumber("032-999999-000001").get();

        System.out.println("===== 낙관적 락 동작 검증 =====");
        System.out.println("성공: " + successCount.get() + "건");
        System.out.println("충돌로 인한 실패: " + failCount.get() + "건");
        System.out.println("최종 잔액: " + finalAccount.getBalance());
        System.out.println("예상 잔액: " +
                BigDecimal.valueOf(100000).subtract(
                        BigDecimal.valueOf(10000).multiply(BigDecimal.valueOf(successCount.get()))));
        System.out.println("================================");

        // 성공한 만큼만 잔액 차감됐는지 검증 → 낙관적 락이 정합성 보장
        BigDecimal expectedBalance = BigDecimal.valueOf(100000)
                .subtract(BigDecimal.valueOf(10000)
                        .multiply(BigDecimal.valueOf(successCount.get())));

        assertThat(finalAccount.getBalance())
                .isEqualTo(expectedBalance);

        // 낙관적 락으로 인해 일부 요청은 반드시 실패해야 함
        assertThat(failCount.get()).isGreaterThan(0);

        System.out.println("✅ 낙관적 락이 동시성 문제를 방지했습니다!");
    }


    @Test
    @DisplayName("동시 이체 요청 시 낙관적 락으로 정합성 보장")
    void concurrentTransferTest() throws InterruptedException {
        // given
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        TransferRequest request = new TransferRequest(
                "032-999999-000001",
                "032-999999-000002",
                BigDecimal.valueOf(10000),
                "동시 이체 테스트"
        );

        // when - 5개 스레드가 동시에 10,000원씩 이체 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    transactionService.transfer(testUser, request);
                    results.add("SUCCESS");
                } catch (Exception e) {
                    results.add("FAIL: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Account finalFromAccount = accountRepository
                .findByAccountNumber("032-999999-000001").get();

        long successCount = results.stream()
                .filter(r -> r.equals("SUCCESS")).count();
        long failCount = results.stream()
                .filter(r -> r.startsWith("FAIL")).count();

        System.out.println("===== 동시 이체 테스트 결과 =====");
        System.out.println("성공: " + successCount + "건");
        System.out.println("실패: " + failCount + "건");
        System.out.println("최종 잔액: " + finalFromAccount.getBalance());
        System.out.println("================================");

        // 성공한 만큼만 잔액이 차감됐는지 검증
        BigDecimal expectedBalance = BigDecimal.valueOf(100000)
                .subtract(BigDecimal.valueOf(10000).multiply(BigDecimal.valueOf(successCount)));

        assertThat(finalFromAccount.getBalance()).isEqualTo(expectedBalance);
    }
}
