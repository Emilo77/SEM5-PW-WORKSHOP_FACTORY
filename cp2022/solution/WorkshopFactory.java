/*
 * University of Warsaw
 * Concurrent Programming Course 2022/2023
 * Java Assignment
 *
 * Author: Konrad Iwanicki (iwanicki@mimuw.edu.pl)
 */
package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;
import cp2022.tests.fibonacci.Worker;
import org.junit.jupiter.api.Assertions;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static cp2022.solution.Utils.Action;
import static cp2022.solution.Utils.getWorkplace;


public final class WorkshopFactory {
    static String runtimeMessage = "panic: unexpected thread interruption";

    public static class WorkerInfo {

        private final String name; //do usunięcia później
        private final Long pid;
        private Action desiredAction;
        private WorkplaceWrapper currentWorkplace;
        private WorkplaceWrapper desiredWorkplace;
        private Semaphore mutex;

        public WorkerInfo() {
            this.name = Thread.currentThread().getName();
            this.pid = Thread.currentThread().getId();
            this.desiredAction = Action.ENTER;
            this.currentWorkplace = null;
            this.desiredWorkplace = null;
            this.mutex = new Semaphore(1, true);
        }

        public void update(Action newAction,
                           WorkplaceWrapper workplace) {

            this.desiredAction = newAction;

            if (newAction.equals(Action.ENTER)) {
                this.currentWorkplace = null;
                this.desiredWorkplace = workplace;
            } else if (newAction.equals(Action.USE)) {
                this.currentWorkplace = workplace;
                this.desiredWorkplace = null;
            }
        }

        public void update(Action newAction,
                           WorkplaceWrapper newCurrentWorkplace,
                           WorkplaceWrapper newDesiredWorkplace) {

            desiredAction = newAction;
            currentWorkplace = newCurrentWorkplace;
            desiredWorkplace = newDesiredWorkplace;
        }
    }

    public static class WorkersOccupyMap {
        ConcurrentHashMap<Long, WorkerInfo> occupyMap;

        Semaphore mutex;

        protected WorkersOccupyMap() {
            this.occupyMap = new ConcurrentHashMap<>();
            this.mutex = new Semaphore(1, true);
        }

        protected void insertNewWorker(Long pid) throws InterruptedException {
            mutex.acquire();

            WorkerInfo info = new WorkerInfo();
            occupyMap.put(pid, info); //sprawdzenie, czy elementu nie ma już w mapie

            mutex.release();
        }

        protected void removeWorker(Long pid) throws InterruptedException {
            mutex.acquire();

            WorkerInfo info = occupyMap.get(pid);

            if (info == null) {
                throw new RuntimeException("panic: couldn't find workplace occupied by this pid! free");
            }
            occupyMap.remove(pid);

            mutex.release();
        }

        protected WorkplaceWrapper getCurrentWorkplace(Long pid) throws InterruptedException {
            mutex.acquire();

            WorkerInfo info = occupyMap.get(pid);
            if (info == null) {
                throw new RuntimeException("Map panic: getCurrentWorkplace: couldn't find workplace occupied by this pid!");
            }

            mutex.release();
            return info.currentWorkplace;
        }

        protected WorkplaceWrapper getDesiredWorkplace(Long pid) throws InterruptedException {
            mutex.acquire();

            WorkerInfo info = occupyMap.get(pid);
            if (info == null) {
                throw new RuntimeException("Map panic: getDesiredWorkplace: couldn't find workplace occupied by this pid! get");
            }

            mutex.release();
            return info.desiredWorkplace;
        }

        protected void updateWorkerInfo(Long pid,
                                        Action action,
                                        WorkplaceWrapper workplace)
                throws InterruptedException {
            mutex.acquire();

            WorkerInfo info = occupyMap.get(pid);
            if (info == null) {
                throw new RuntimeException("Map panic: getDesiredWorkplace: couldn't find workplace occupied by this pid! get");
            }

            Assertions.assertNotEquals(Action.SWITCHTO, action);

            info.update(action, workplace);

            mutex.release();
        }

        protected void updateWorkerInfo(Long pid,
                                        Action action,
                                        WorkplaceWrapper currentWorkplace,
                                        WorkplaceWrapper desiredWorkplace)
                throws InterruptedException {
            mutex.acquire();

            WorkerInfo info = occupyMap.get(pid);
            if (info == null) {
                throw new RuntimeException("Map panic: getDesiredWorkplace: couldn't find workplace occupied by this pid! get");
            }

            Assertions.assertEquals(Action.SWITCHTO, action);

            info.update(action, currentWorkplace, desiredWorkplace);

            mutex.release();
        }

    }

    public static Collection<WorkplaceWrapper> initializeWrappers(
            Collection<Workplace> workplaces,
            WorkersOccupyMap workerMap) {
        return workplaces.stream().map(workplace -> new WorkplaceWrapper(workplace, workerMap)
        ).collect(Collectors.toList());
    }

    static class WorkplaceWrapper extends Workplace {
        private final Workplace workplace;
        private final Semaphore enterMutex;
        private final Semaphore workingMutex;
        private final WorkersOccupyMap workerMap;
        private final Set<Long> workers;

        protected WorkplaceWrapper(Workplace workplace, WorkersOccupyMap workerMap) {
            super(workplace.getId());
            this.workplace = workplace;
            this.enterMutex = new Semaphore(1, true);
            this.workingMutex = new Semaphore(1, true);
            this.workerMap = workerMap;
            this.workers = new HashSet<>();
        }

        private void addWorker(Long pid) {
            workers.add(pid);
        }

        private void removeWorker(Long pid) {
            workers.remove(pid);
        }

        @Override
        public void use() {
            try {
                this.workers.add(Thread.currentThread().getId());
                /* Informacja o wątku, że chce zacząć pracę na stanowisku */
                workerMap.updateWorkerInfo(
                        Thread.currentThread().getId(),
                        Action.USE,
                        this);

                workingMutex.acquire();
                workplace.use();
            } catch (InterruptedException e) {
                throw new RuntimeException(runtimeMessage);
            }
        }
    }

//    private static class WorkerSemaphore {
//        private final Long pid;
//        private final Semaphore semaphore;
//
//        protected WorkerSemaphore(Long pid, Integer permits) {
//            this.pid = pid;
//            this.semaphore = new Semaphore(permits, true);
//        }
//    }
//
//    private static class WaitingQue {
//        private final Deque<WorkerSemaphore> que;
//        private final Integer permits;
//        private final Semaphore mutex;
//
//        protected WaitingQue(Integer permits) {
//            this.que = new ArrayDeque<>();
//            this.permits = permits;
//            this.mutex = new Semaphore(1, true);
//        }
//
//        protected void add(Long pid) throws InterruptedException {
//            mutex.acquire();
//            WorkerSemaphore workerSemaphore = new WorkerSemaphore(pid, permits);
//            que.add(workerSemaphore);
//            mutex.release();
//        }
//
//        protected void remove() throws InterruptedException {
//            mutex.acquire();
//            WorkerSemaphore firstSemaphore = que.peek();
//            Assertions.assertNotEquals(null, firstSemaphore);
//            firstSemaphore.semaphore.release(permits + 1);
//            que.remove();
//            mutex.release();
//        }
//
//        protected void passQue(Long pid) throws InterruptedException {
//            mutex.acquire();
//
//            WorkerSemaphore semaphore;
//            Iterator<WorkerSemaphore> iterator = que.descendingIterator();
//
//            for (Iterator<WorkerSemaphore> it = que.descendingIterator(); it.hasNext(); ) {
//                semaphore = it.next();
//                if (Objects.equals(pid, semaphore.pid)) {
//                    iterator = it;
//                    break;
//                }
//            }
//            while (iterator.hasNext()) {
//                semaphore = iterator.next();
//                //tutaj jakoś oznaczyć pid jako zatrzymany, aby dało się go obudzić
//                semaphore.semaphore.acquire();
//            }
//            mutex.release();
//        }
//
//
//    }

    public final static Workshop newWorkshop(Collection<Workplace> workplaces) {


        return new Workshop() {
            private final WorkersOccupyMap workerMap = new WorkersOccupyMap();
            private final Collection<WorkplaceWrapper> workplaceWrappers =
                    initializeWrappers(workplaces, workerMap);
            private final WaitingQue waitingQue =
                    new WaitingQue(workplaceWrappers.size());

            @Override
            public Workplace enter(WorkplaceId wid) {
                try {

                    workerMap.insertNewWorker(Thread.currentThread().getId());
                    /* Wyszukanie stanowiska o podanym identyfikatorze */
                    WorkplaceWrapper workplace = getWorkplace(workplaceWrappers, wid);

                    /* Nowa informacja o wątku, że chce wejść na stanowisko */
                    workerMap.updateWorkerInfo(
                            Thread.currentThread().getId(),
                            Action.ENTER,
                            workplace);

//                    waitingQue.passQue(Thread.currentThread().getId());

                    /* Zawieszenie się na wejściu na stanowisko */
                    workplace.enterMutex.acquire();

                    return workplace;
                } catch (InterruptedException e) {
                    throw new RuntimeException(runtimeMessage);
                }
            }

            private void checkCycle(WorkplaceWrapper workplace) {

            }

            @Override
            //todo sytuacja, kiedy robimy switchTo na to samo stanowisko
            public Workplace switchTo(WorkplaceId wid) {
                try {

                    /* Wyszukanie aktualnego stanowiska */
                    WorkplaceWrapper currentWorkplace =
                            workerMap.getCurrentWorkplace(Thread.currentThread().getId());

                    /* Wyszukanie następnego stanowiska */
                    WorkplaceWrapper futureWorkplace = getWorkplace(workplaceWrappers, wid);

                    /* Jeżeli worker chce zmienić stanowisko na takie samo,
                     * posiada priorytet i od razu może ponownie wykonać USE. */
                    if (currentWorkplace.getId() == futureWorkplace.getId()) {
                        /* Nowa informacja, że wątek chce wykonać USE. */
                        workerMap.updateWorkerInfo(
                                Thread.currentThread().getId(),
                                Action.USE,
                                futureWorkplace);
                        currentWorkplace.workingMutex.release();
                        return futureWorkplace;
                    }

                    /* Nowa informacja o wątku, że chce zmienić stanowisko. */
                    workerMap.updateWorkerInfo(
                            Thread.currentThread().getId(),
                            Action.SWITCHTO,
                            currentWorkplace,
                            futureWorkplace);

                    checkCycle(futureWorkplace);

                    currentWorkplace.enterMutex.release();
                    futureWorkplace.enterMutex.acquire();

                    currentWorkplace.removeWorker(Thread.currentThread().getId());
                    futureWorkplace.addWorker(Thread.currentThread().getId());

                    /* Nowa informacja o wątku, że zmienił stanowisko
                     * i chce teraz użyć metody USE. */
                    workerMap.updateWorkerInfo(
                            Thread.currentThread().getId(),
                            Action.USE,
                            futureWorkplace
                    );
                    currentWorkplace.workingMutex.release();

                    return futureWorkplace;

                } catch (InterruptedException e) {
                    throw new RuntimeException(runtimeMessage);
                }
            }

            @Override
            public void leave() {
                try {
                    WorkplaceWrapper workplace = workerMap.getCurrentWorkplace(
                            Thread.currentThread().getId());
                    workplace.workers.remove(Thread.currentThread().getId());
                    workerMap.removeWorker(Thread.currentThread().getId());
                    workplace.enterMutex.release();
                    workplace.workingMutex.release();
                } catch (InterruptedException e) {
                    throw new RuntimeException(runtimeMessage);
                }

            }
        };
    }
}
