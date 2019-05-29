package org.csc.account.sample;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.csc.account.util.ALock;
import org.csc.evmapi.gens.Tx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "TransactionLoadTest_Store")
@Slf4j
@Data
public class TransactionLoadTestStore implements ActorService {
	private List<Tx.Transaction.Builder> loads = new ArrayList<>();
	private AtomicInteger used_idx = new AtomicInteger(-1);
	private int loopCount = 0;
	protected ReadWriteLock rwLock = new ReentrantReadWriteLock();
	protected ALock readLock = new ALock(rwLock.readLock());

	public Tx.Transaction.Builder getOne() {
		try {
			return loads.get(used_idx.incrementAndGet());
		} catch (Exception e) {
			return null;
		}
	}

	public void clear() {
		loads.clear();
		used_idx.set(-1);
	}

	public int remain() {
		return loads.size() - (used_idx.get() + 1);
	}
	public synchronized void addTx(Tx.Transaction.Builder mb){
		loads.add(mb);
	}
}
