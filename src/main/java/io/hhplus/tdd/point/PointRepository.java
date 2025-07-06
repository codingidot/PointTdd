package io.hhplus.tdd.point;

import java.util.List;

import org.springframework.stereotype.Repository;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

@Repository
public class PointRepository {
	
	private final UserPointTable userPointTable;
	private final PointHistoryTable pointHistoryTable;
	
	public PointRepository(UserPointTable userPointTable, PointHistoryTable pointHistoryTable){
		this.userPointTable = userPointTable;
		this.pointHistoryTable = pointHistoryTable;
	}

	//특정 유저의 포인트를 조회
	public UserPoint selectUserById(long id) {
		return userPointTable.selectById(id);
	}

	//특정 유저의 포인트를 충전 또는 사용
	public UserPoint updatePoint(long id, long totalAmount) {
		return userPointTable.insertOrUpdate(id, totalAmount);
	}

	//특정 유저의 포인트 충전/이용 내역을 조회
	public List<PointHistory> selectHistoryById(long id) {
		return pointHistoryTable.selectAllByUserId(id);
	}

	//히스토리에 등록
	public void addHistory(long id, long amount, TransactionType charge) {
		pointHistoryTable.insert(id, amount, charge, System.currentTimeMillis());
	}
}
