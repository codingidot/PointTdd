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
	
	@BeforeEach
    void setUp() {
		UserPointTable userPointTable = new UserPointTable();
		PointHistoryTable pointHistoryTable = new PointHistoryTable();
        PointRepository pointRepository = new PointRepository(userPointTable, pointHistoryTable);
        pointService = new PointService(pointRepository);
    }
	
	//유저 포인트 충전
	@Test
	public void chargePoint() {
		//user , point 등록
		long id1 = 1;
		long amount1 = 100;
		long amount2 = 200;
		UserPoint user1;
				
		user1 = pointService.chargeUserPoint(id1, amount1);
		Assertions.assertEquals(amount1, user1.point(),"충전한 금액 일치 확인");//100
		user1 = pointService.chargeUserPoint(id1, amount2);
		Assertions.assertEquals(amount1+amount2, user1.point(),"충전한 금액 일치 확인");//300
	}
	
	//유저 포인트 사용
	@Test
	public void usePoint() {
		//user , point 등록
		long id1 = 1;
		long amount1 = 300;
		long amount2 = 200;
		long amount3 = 100;

		//충전
		pointService.chargeUserPoint(id1, amount1);
		
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
		//user , point 등록
		long id1 = 1;
		long amount1 = 400;
		UserPoint user1;
		
		pointService.chargeUserPoint(id1, amount1);
		user1 = pointService.selectUserById(id1);
		
		Assertions.assertEquals(id1, user1.id(), "조회한 id 확인");
		Assertions.assertEquals(amount1, user1.point(), "충전한 금액과 일치하는지 확인");
	}
	
	//특정 유저의 포인트 충전/이용 내역을 조회
	@Test
	public void selectUserPointHistory() {
		
		//user , point 등록
		long id1 = 1;
		
		//충전 및 사용
		pointService.chargeUserPoint(id1, 3000);
		pointService.chargeUserPoint(id1, 600);
		Assertions.assertDoesNotThrow(() -> {
			pointService.useUserPoint(id1, 200);
		});
		pointService.chargeUserPoint(id1, 400);
		
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
}
