package cp2022.solution;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

import java.lang.reflect.Array;
import java.time.chrono.ThaiBuddhistEra;
import java.util.*;
import java.util.concurrent.Semaphore;

class FreezeMap {
    Map<Long, Set<Long>> map;
    Semaphore mutex = new Semaphore(1, true);

    FreezeMap() {
        map = new HashMap<>();
    }

    public void getFreezedBy(Long pid) throws InterruptedException {
        mutex.acquire();
        map.computeIfAbsent(pid, k -> new HashSet<>());

        map.get(pid).add(Thread.currentThread().getId());
        mutex.release();
    }
    public Set<Long> getSetToUnfreeze() throws InterruptedException {
        mutex.acquire();
        Set<Long> res =  map.get(Thread.currentThread().getId());
        mutex.release();
        return res;
    }

    public void remove(Long pid) {
        map.remove(pid);
    }
}

class WorkerPatience {
     Long pid;
     Integer patience;

    protected WorkerPatience(Integer permits) {
        this.pid = Thread.currentThread().getId();
        this.patience = permits;
    }
}

class WaitingQue {
        private final Semaphore mutex;
        private final Integer permits;
        private final FreezeMap freezeMap;
        private final ArrayList<WorkerPatience> array;
        private final Map<Long, Semaphore> semaphoreMap;

    protected WaitingQue(Integer permits) {
        this.mutex = new Semaphore(1, true);
        this.permits = permits;
        this.freezeMap = new FreezeMap();
        this.array = new ArrayList<>();
        this.semaphoreMap = new HashMap<>();
    }

    protected void add(Long pid) throws InterruptedException {
        mutex.acquire();

        WorkerPatience newSemaphore = new WorkerPatience(permits);
        semaphoreMap.put(pid, new Semaphore(0, false));
        array.add(0, newSemaphore);

        mutex.release();
    }

    protected Integer getIndex(Long pid) {
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).pid.equals(pid)) {
                return i;
            }
        }
        return null;
    }

    protected void pass(String name, Long pid) throws InterruptedException {
        mutex.acquire();

        Integer index = getIndex(pid);
        Assertions.assertNotEquals(null, index);

        if (index + 1 > array.size() - 1) {
            mutex.release();
            return;
        }

        for(int i = index + 1; i < array.size(); i++) {
            if (array.get(i).patience == 0) {
                freezeMap.getFreezedBy(array.get(i).pid);

                Semaphore s = semaphoreMap.get(array.get(i).pid);

                System.out.println(Thread.currentThread().getId() + " zfreezował się na " + array.get(i).pid);

                mutex.release();
                s.acquire();
//                System.out.println(Thread.currentThread().getId() + " odfreezował się z " + array.get(i).pid);


                pass(name, pid);
                return;
            } else {
                array.get(i).patience--;
            }

        }
        mutex.release();

    }

    protected void remove(Long pid) throws InterruptedException {
        mutex.acquire();

        Integer index = getIndex(pid);
        Assertions.assertNotEquals(null, index);
        WorkerPatience semaphore = array.get(index);



        semaphoreMap.get(Thread.currentThread().getId()).release(permits * 2);



        freezeMap.remove(pid);
        array.remove(semaphore);

        mutex.release();
    }

}



public class Main {

    static String printThread() {
        return "[" + Thread.currentThread().getId() + "]";
    }
    static Integer PATIENCE = 10;
    static WaitingQue que = new WaitingQue(PATIENCE);

    public static Thread createThread(Integer sleepTime, String name, ArrayList<Long> start, ArrayList<Long> res) {
        Semaphore mutex = new Semaphore(1,true);
        return new Thread(() -> {
            try {
                mutex.acquire();


                que.add(Thread.currentThread().getId());

                start.add(Thread.currentThread().getId());
                System.out.println(printThread() + " dodał się do kolejki");

                mutex.release();

                mutex.acquire();

                que.pass(name, Thread.currentThread().getId());
                res.add(Thread.currentThread().getId());

                System.out.println(printThread() + " przeszedł przez kolejkę");


                mutex.release();

                mutex.acquire();
                que.remove(Thread.currentThread().getId());
                System.out.println(printThread() + " usunął się z kolejki");
                mutex.release();

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, name);
    }

    static Integer findIndex(ArrayList<Long> array, Long element) {
        for(int i = 0; i < array.size(); i++) {
            if (array.get(i).equals(element)) {
                return i;
            }
        }
        return -1;
    }

    @RepeatedTest(10000)
    public  void queTest() throws InterruptedException {

        ArrayList<Long> result = new ArrayList<>();
        ArrayList<Long> start = new ArrayList<>();

        int number = 20;

        Thread[] threads = new Thread[number];

        for (int i = 0; i < number; i++) {
            threads[i] = createThread(1000 - i * 100, "", start, result);
        }

        for (Thread t: threads) {
            t.start();
        }

        for (Thread t: threads) {
            t.join();
        }

        System.out.println(start);
        System.out.println(result);
        for(int i = 0; i < start.size(); i++) {
            int found = findIndex(result, start.get(i));
            if (found - i > PATIENCE) {
                System.out.println("Failed at index i: " + i);
                System.out.println("Index at end: " + found);
                Assertions.fail();
            }
        }

    }

//    publis static void t
    public static void main(String[] args) throws InterruptedException {
//        queTest();
    }
}