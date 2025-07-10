package io.hhplus.tdd.point;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class PointService {
	
	private final PointRepository pointRepository;
	private final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();

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
		ReentrantLock lock = lockMap.computeIfAbsent(id, lockId -> new ReentrantLock());
		lock.lock();
        try {
        	UserPoint userPoint = pointRepository.selectUserById(id);
    		long totalAmount = userPoint.point() + amount;
        	if(totalAmount > 10000000) {
    			throw new Exception("포인트는 천만원을 초과하지 못합니다.");
    		}
    		userPoint = pointRepository.updatePoint(id, totalAmount);
    		pointRepository.addHistory(id, amount, TransactionType.CHARGE);
    		return userPoint;
        } finally {
            lock.unlock();
            // 락이 사용 중이지 않을 때만 제거(메모리 낭비 방지)
            if (!lock.hasQueuedThreads()) { // 기다리는 스레드 없으면
                lockMap.remove(id, lock);
            }
        }
	}

	//특정 유저의 포인트를 사용
	public UserPoint useUserPoint(long id, long amount) throws Exception {
		ReentrantLock lock = lockMap.computeIfAbsent(id, lockId -> new ReentrantLock());
		lock.lock();
        try {
        	UserPoint userPoint = pointRepository.selectUserById(id);
        	if(userPoint.point() >= amount) {
    			long totalAmount = userPoint.point() - amount;
    			userPoint = pointRepository.updatePoint(id, totalAmount);
    			pointRepository.addHistory(id, amount, TransactionType.USE);
    			return userPoint;
    		}else {
    			throw new Exception("포인트가 부족합니다.");
    		}
        } finally {
            lock.unlock();
            // 락이 사용 중이지 않을 때만 제거(메모리 낭비 방지)
            if (!lock.hasQueuedThreads()) { // 기다리는 스레드 없으면
                lockMap.remove(id, lock);
            }
        }
	}
	
	//특정 유저의 포인트 충전/이용 내역을 조회
	public List<PointHistory> selectHistoryById(long id) {
		return pointRepository.selectHistoryById(id);
	}
	
}
