package io.hhplus.tdd.point;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class PointService {
	
	private final PointRepository pointRepository;
	
	@Autowired
	public PointService(PointRepository pointRepository){
		this.pointRepository = pointRepository;
	}

	//특정 유저의 포인트를 조회
	public UserPoint selectUserById(long id) {
		UserPoint userPoint = pointRepository.selectUserById(id);
		return userPoint;
	}

	//특정 유저의 포인트를 충전
	public UserPoint chargeUserPoint(long id, long amount) throws Exception {
		UserPoint userPoint = pointRepository.selectUserById(id);
		long totalAmount = userPoint.point() + amount;
		if(totalAmount > 10000000) {
			throw new Exception("포인트는 천만원을 초과하지 못합니다.");
		}
		userPoint = pointRepository.updatePoint(id, totalAmount);
		pointRepository.addHistory(id, amount, TransactionType.CHARGE);
		return userPoint;
	}

	//특정 유저의 포인트를 사용
	public UserPoint useUserPoint(long id, long amount) throws Exception {
		UserPoint userPoint = pointRepository.selectUserById(id);
		if(userPoint.point() >= amount) {
			long totalAmount = userPoint.point() - amount;
			userPoint = pointRepository.updatePoint(id, totalAmount);
			pointRepository.addHistory(id, amount, TransactionType.USE);
			return userPoint;
		}else {
			throw new Exception("포인트가 부족합니다.");
		}
	}
	
	//특정 유저의 포인트 충전/이용 내역을 조회
	public List<PointHistory> selectHistoryById(long id) {
		return pointRepository.selectHistoryById(id);
	}
	
}
