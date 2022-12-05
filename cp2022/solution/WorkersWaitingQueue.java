package cp2022.solution;

import java.util.*;
import java.util.concurrent.Semaphore;

public class WorkersWaitingQueue {

    private class WorkerPatience {
        private Long pid;
        private Integer patience;

        public WorkerPatience(Integer patience) {
            this.pid = Thread.currentThread().getId();
            this.patience = patience;
        }

        public void decrement() {
            patience--;
        }
        public Integer getPatience() {
            return patience;
        }

        public Long getPid() {
            return pid;
        }
    }

    private class FreezeMap {
        private final Map<Long, Set<Long>> freezeMap;
        private final Map<Long, Semaphore> semaphoreMap;
        private final Semaphore mapBlocker;

        protected FreezeMap() {
            this.freezeMap = new HashMap<>();
            this.mapBlocker = new Semaphore(1, true);
            this.semaphoreMap = new HashMap<>();
        }

        protected void insertSemaphore(Long pid) {
            semaphoreMap.put(pid, new Semaphore(0, false));
        }

        protected Semaphore getSemaphore(Long pid) {
            return semaphoreMap.get(pid);
        }

        protected void getFreezedBy(Long pid) throws InterruptedException {
            mapBlocker.acquire();

            freezeMap.computeIfAbsent(pid, k -> new HashSet<>());
            freezeMap.get(pid).add(Thread.currentThread().getId());

            mapBlocker.release();
        }

        public void remove(Long pid) {
            freezeMap.remove(pid);
            semaphoreMap.remove(pid);
        }

    }

    private final Semaphore queBlocker;
    private final Integer permits;
    private final FreezeMap freezeMap;
    private final ArrayList<WorkerPatience> patienceArray;

    protected WorkersWaitingQueue(Integer permits) {
        this.queBlocker = new Semaphore(1, true);
        this.permits = permits;
        this.freezeMap = new FreezeMap();
        this.patienceArray = new ArrayList<>();
    }

    protected void add(Long pid) throws InterruptedException {
        queBlocker.acquire();

        WorkerPatience patience = new WorkerPatience(permits);

        freezeMap.insertSemaphore(pid);
        patienceArray.add(0, patience);

        queBlocker.release();
    }

    protected Integer getIndexOfPatience(Long pid) {
        for (int i = 0; i < patienceArray.size(); i++) {
            if (patienceArray.get(i).pid.equals(pid)) {
                return i;
            }
        }
        return null;
    }

    protected void pass(Long pid) throws InterruptedException {
        queBlocker.acquire();

        Integer patienceIndex = getIndexOfPatience(pid);

        if (patienceIndex + 1 > patienceArray.size() - 1) {
            queBlocker.release();
            return;
        }

        for(int i = patienceIndex + 1; i < patienceArray.size(); i++) {
            WorkerPatience workerPatience = patienceArray.get(i);

            if(workerPatience.getPatience() == 0) {
                freezeMap.getFreezedBy(workerPatience.getPid());

                Semaphore semaphoreToFreeze =
                        freezeMap.getSemaphore(workerPatience.getPid());

                queBlocker.release();

                semaphoreToFreeze.acquire();

                this.pass(pid);
            } else {
                workerPatience.decrement();
            }
        }

    }

    protected void remove(Long pid) throws InterruptedException {
        queBlocker.acquire();

        Integer indexToRemove = getIndexOfPatience(pid);

        WorkerPatience patienceToRemove = patienceArray.get(indexToRemove);

        freezeMap.getSemaphore(Thread.currentThread().getId()).release(permits + 1);

        freezeMap.remove(pid);
        patienceArray.remove(patienceToRemove);

        queBlocker.release();
    }

}
