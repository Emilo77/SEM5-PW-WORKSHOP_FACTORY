package cp2022.solution;


import java.util.*;
import java.util.concurrent.Semaphore;

/* Klasa kolejki, użyta do wyeliminowania zagłodzenia wątków. */
public class WorkersWaitingQueue {
    private final Integer permits;
    private final FreezeMap freezeMap;
    private final ArrayList<WorkerPatience> patienceArray;
    private final Semaphore queBlocker;

    protected WorkersWaitingQueue(Integer permits) {
        this.permits = permits;
        this.freezeMap = new FreezeMap();
        this.patienceArray = new ArrayList<>();
        this.queBlocker = new Semaphore(1, true);
    }

    /* Zwrócenie indeksu w tablicy. */
    private Integer getIndexOfPatience(Long pid) {
        for (int i = 0; i < patienceArray.size(); i++) {
            if (patienceArray.get(i).pid.equals(pid)) {
                return i;
            }
        }
        throw new NullPointerException();
    }

    /* Dodanie wątku do kolejki. */
    protected void add(Long pid) throws InterruptedException {
        queBlocker.acquire();

        WorkerPatience patience = new WorkerPatience(permits);

        freezeMap.insertSemaphore(pid);
        patienceArray.add(0, patience);

        queBlocker.release();
    }

    /* Przejście wątku przez kolejkę. */
    protected void pass(Long pid) throws InterruptedException {
        queBlocker.acquire();

        Integer patienceIndex = getIndexOfPatience(pid);

        /* Jeżeli wątek jest pierwszy w kolejce lub już poza nią,
        * możemy go przepuścić. */
        if (patienceIndex + 1 > patienceArray.size() - 1) {
            queBlocker.release();
            return;
        }

        for(int i = patienceIndex + 1; i < patienceArray.size(); i++) {
            WorkerPatience workerPatience = patienceArray.get(i);

            /* Jeżeli następny wątek posiada cierpliwość równą 0,
            * zatrzymujemy się na jego semaforze.
            * Kiedy ten wątek wyjdzie z kolejki,
            * obudzi nas i będziemy mogli próbować przejść przez kolejne wątki. */
            if(workerPatience.getPatience() == 0) {
                freezeMap.getFreezedBy(workerPatience.getPid());

                Semaphore semaphoreToFreeze =
                        freezeMap.getSemaphore(workerPatience.getPid());

                queBlocker.release();

                semaphoreToFreeze.acquire();

                this.pass(pid);
                return;

            } else {
                workerPatience.decrement();
            }
        }

        queBlocker.release();
    }

    /* Usunięcie wątku z kolejki. */
    protected void remove(Long pid) throws InterruptedException {
        queBlocker.acquire();

        Integer indexToRemove = getIndexOfPatience(pid);

        WorkerPatience patienceToRemove = patienceArray.get(indexToRemove);

        freezeMap.getSemaphore(Thread.currentThread().getId()).release(permits + 1);

        freezeMap.remove(pid);
        patienceArray.remove(patienceToRemove);

        queBlocker.release();
    }

    /* Klasa pomocnicza, zwracająca informacje,
    * ile wątek może przepuścić innych wątków.
    * Liczbę wątków, jakie wątek może przepuścić, nazywam cierpliwością wątku. */
    private static class WorkerPatience {
        private final Long pid;
        private Integer patience;

        public WorkerPatience(Integer patience) {
            this.pid = Thread.currentThread().getId();
            this.patience = patience;
        }

        /* Zmniejszenie cierpliwości wątku. */
        public void decrement() {
            patience--;
        }

        /* Zwrócenie cierpliwości wątku. */
        public Integer getPatience() {
            return patience;
        }

        /* Zwrócenie identyfikatora wątku. */
        public Long getPid() {
            return pid;
        }
    }

    /* Klasa pomocnicza mapy do zatrzymywania wątków oczekujących w kolejce. */
    private static class FreezeMap {
        private final Map<Long, Set<Long>> freezeMap;
        private final Map<Long, Semaphore> semaphoreMap;
        private final Semaphore mapBlocker;

        protected FreezeMap() {
            this.freezeMap = new HashMap<>();
            this.mapBlocker = new Semaphore(1, true);
            this.semaphoreMap = new HashMap<>();
        }

        protected void insertSemaphore(Long pid) {
            semaphoreMap.put(pid, new Semaphore(0, true));
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

        public void remove(Long pid) throws InterruptedException {
            mapBlocker.acquire();

            freezeMap.remove(pid);
            semaphoreMap.remove(pid);

            mapBlocker.release();
        }

    }

}

//
//
//class essa {
//
//    static String printThread() {
//        return "[" + Thread.currentThread().getId() + "]";
//    }
//    static Integer PATIENCE = 2;
//    static WorkersWaitingQueue que = new WorkersWaitingQueue(PATIENCE);
//
//    public static Thread createThread(Integer sleepTime, String name, ArrayList<Long> start, ArrayList<Long> res) {
//        Semaphore mutex = new Semaphore(1,true);
//        return new Thread(() -> {
//            try {
//                mutex.acquire();
//
//
//                que.add(Thread.currentThread().getId());
//
//                start.add(Thread.currentThread().getId());
//
//
//                mutex.release();
//
//                mutex.acquire();
//
//                que.pass(Thread.currentThread().getId());
//                res.add(Thread.currentThread().getId());
//
//
//
//
//                mutex.release();
//
//                mutex.acquire();
//                que.remove(Thread.currentThread().getId());
//
//                mutex.release();
//
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }, name);
//    }
//
//    static Integer findIndex(ArrayList<Long> array, Long element) {
//        for(int i = 0; i < array.size(); i++) {
//            if (array.get(i).equals(element)) {
//                return i;
//            }
//        }
//        return -1;
//    }
//
//    @RepeatedTest(10000)
//    public  void queTest() throws InterruptedException {
//
//        ArrayList<Long> result = new ArrayList<>();
//        ArrayList<Long> start = new ArrayList<>();
//
//        int number = 100;
//
//        Thread[] threads = new Thread[number];
//
//        for (int i = 0; i < number; i++) {
//            threads[i] = createThread(1000 - i * 100, "", start, result);
//        }
//
//        for (Thread t: threads) {
//            t.start();
//        }
//
//        for (Thread t: threads) {
//            t.join();
//        }
//
//        System.out.println(start);
//        System.out.println(result);
//        for(int i = 0; i < start.size(); i++) {
//            int found = findIndex(result, start.get(i));
//            if (found - i > PATIENCE) {
//                System.out.println("Failed at index i: " + i);
//                System.out.println("Index at end: " + found);
//                Assertions.fail();
//            }
//        }
//
//    }
//
//    //    publis static void t
//    public static void main(String[] args) throws InterruptedException {
////        queTest();
//    }
//}