package io.hhplus.tdd;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointRepository;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;

public class PointTest {

	PointService pointService;
	long id1 = 1;
	
	@BeforeEach
    void setUp() {
		//각 테스트 메서드마다 데이터 초기화
		UserPointTable userPointTable = new UserPointTable();
		PointHistoryTable pointHistoryTable = new PointHistoryTable();
        PointRepository pointRepository = new PointRepository(userPointTable, pointHistoryTable);
        pointService = new PointService(pointRepository);
    }
	
	//유저 포인트 충전
	@Test
	public void chargePoint() {

		long amount1 = 100;
		long amount2 = 200;
		
		//충전되는 금액 확인
		Assertions.assertDoesNotThrow(() -> {
			pointService.chargeUserPoint(id1, amount1);
		},"충전 금액이 천만원을 넘지 않으면 에러가 발생하지 않음");
		
		Assertions.assertEquals(amount1, pointService.selectUserById(id1).point(),"충전한 금액 일치 확인");//100
		
		Assertions.assertDoesNotThrow(() -> {
			pointService.chargeUserPoint(id1, amount2);
		},"충전 금액이 천만원을 넘지 않으면 에러가 발생하지 않음");
		
		Assertions.assertEquals(amount1+amount2, pointService.selectUserById(id1).point(),"충전한 금액 일치 확인");//300
		
		//천만원 초과 충전시 에러 발생
		Exception e = Assertions.assertThrows(
		        Exception.class,
		        () -> pointService.chargeUserPoint(id1, 9999701)//300 + 9999701 = 10000001 
		    ,"천만을 초과하여 Exception throw");
		
		Assertions.assertEquals("포인트는 천만원을 초과하지 못합니다.", e.getMessage(), "포인트 초과 충전시 날린 Exception 메시지 확인");		
	}
	
	//유저 포인트 사용
	@Test
	public void usePoint() {

		long amount1 = 300;
		long amount2 = 200;
		long amount3 = 100;

		//충전
		Assertions.assertDoesNotThrow(() -> {
			pointService.chargeUserPoint(id1, amount1);
		},"충전 금액이 천만원을 넘지 않으면 에러가 발생하지 않음");
		
		//사용
		Assertions.assertDoesNotThrow(() -> {
			Assertions.assertEquals(amount1 - amount2, pointService.useUserPoint(id1, amount2).point(), "포인트 사용하 잔액 확인");//300 - 200 = 100
			Assertions.assertEquals(amount1 - amount2 - amount3, pointService.useUserPoint(id1, amount3).point(), "포인트 사용하 잔액 확인");//300 - 200 - 100 = 0
        },"포인트 사용시 잔액이 충분하여 Exception 발생하지 않음");
		
		//초과 사용
		Exception e = Assertions.assertThrows(
	        Exception.class,
	        () -> pointService.useUserPoint(id1, 200) // 0원에서 200원 사용 시도
	    ,"잔액을 초과하여 Exception throw");
		
		Assertions.assertEquals("포인트가 부족합니다.", e.getMessage(), "포인트가 부족한 경우 날린 Exception 메시지 확인");
	}

	//특정 유저의 포인트를 조회
	@Test
	public void selectPointById() {

		long amount1 = 400;
		UserPoint user1;
		
		//층전
		Assertions.assertDoesNotThrow(() -> {
			pointService.chargeUserPoint(id1, amount1);
		});
		
		user1 = pointService.selectUserById(id1);
		
		Assertions.assertEquals(id1, user1.id(), "조회한 id 확인");
		Assertions.assertEquals(amount1, user1.point(), "충전한 금액과 일치하는지 확인");
	}
	
	//특정 유저의 포인트 충전/이용 내역을 조회
	@Test
	public void selectUserPointHistory() {

		//충전 및 사용
		Assertions.assertDoesNotThrow(() -> {
			pointService.chargeUserPoint(id1, 3000);
			pointService.chargeUserPoint(id1, 600);
			pointService.useUserPoint(id1, 200);
			pointService.chargeUserPoint(id1, 400);
		});
		
		List<PointHistory> history = pointService.selectHistoryById(id1);
		//히스토리 등록 시간 내림차순 정렬
		List<PointHistory> sorted = history.stream()
			    .sorted(Comparator.comparing(PointHistory::updateMillis).reversed())
			    .collect(Collectors.toList());
		
		Assertions.assertEquals(4, sorted.size(), "총 4개의 이력이 존재");

	    boolean allMatch = sorted.stream().allMatch(h -> h.userId() == id1);
	    Assertions.assertTrue(allMatch, "모든 이력의 ID가 일치");
	    
	    //가장 최근 히스토리 확인
	    PointHistory ph = sorted.get(0);
	    Assertions.assertEquals(TransactionType.CHARGE, ph.type(),"충전 또는 사용인지 확인");
	    Assertions.assertEquals(400, ph.amount(),"사용 금액 확인");
	}
	
	//충전 동시성 제어 테스트
	@Test
	public void testConcurrent() {
		int threadCount = 100;
        // 초기 포인트 세팅(1000포인트 충전)
		Assertions.assertDoesNotThrow(() -> {
			pointService.chargeUserPoint(id1, 1000);
		});
		Assertions.assertEquals(1000, pointService.selectUserById(id1).point());
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
        	//100번 돌면 총 500포인트 사용 
            threads[i] = new Thread(() -> {
            	Assertions.assertDoesNotThrow(() -> {
            		pointService.useUserPoint(id1, 5);//5포인트 사용
            	});
            });
        }

        // 스레드 시작
        for (Thread t : threads) {
            t.start();
        }

        // 스레드 종료 대기
        for (Thread t : threads) {
        	Assertions.assertDoesNotThrow(() -> {
        		t.join();
        	});
        }

		Assertions.assertEquals(500, pointService.selectUserById(id1).point());
	}
	
	//포인트 사용 동시성 제어 테스트
	@Test
	public void testConcurrent2() {
		int threadCount = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
        	//100번 돌면 총 500포인트 충전 
            threads[i] = new Thread(() -> {
            	Assertions.assertDoesNotThrow(() -> {
            		pointService.chargeUserPoint(id1, 5);
            	});
            });
        }

        // 스레드 시작
        for (Thread t : threads) {
            t.start();
        }

        // 스레드 종료 대기
        for (Thread t : threads) {
        	Assertions.assertDoesNotThrow(() -> {
        		t.join();
        	});
        }

		Assertions.assertEquals(500, pointService.selectUserById(id1).point());
	}
}
