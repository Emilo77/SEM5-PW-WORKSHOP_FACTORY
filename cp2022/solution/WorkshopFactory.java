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

import java.util.*;
import java.util.concurrent.*;

import static cp2022.solution.Utils.Action;
import static cp2022.solution.Utils.getWorkplace;


public final class WorkshopFactory {
    static String runtimeMessage = "panic: unexpected thread interruption";

    static class WorkplaceWrapper extends Workplace {
        private final Workplace workplace;
        private final Semaphore enterMutex;
        private final Semaphore setBlocker;
        private final Semaphore workingMutex;
        private final WorkersOccupyMap workerMap;
        private final Set<Long> workers;

        protected WorkplaceWrapper(Workplace workplace, WorkersOccupyMap workerMap) {
            super(workplace.getId());
            this.workplace = workplace;
            this.enterMutex = new Semaphore(1, true);
            this.workingMutex = new Semaphore(1, true);
            this.setBlocker = new Semaphore(1, true);
            this.workerMap = workerMap;
            this.workers = new HashSet<>();
        }

        private void addWorker(Long pid) throws InterruptedException {
            setBlocker.acquire();

            workers.add(pid);

            setBlocker.release();
        }

        private void removeWorker(Long pid) throws InterruptedException {
            setBlocker.acquire();

            workers.remove(pid);

            setBlocker.release();
        }

        public Set<Long> getWorkers() {
            return workers;
        }

        @Override
        public void use() {
            try {

                /* Jeżeli wątek był wcześniej w metodzie SwitchTo, trzeba zaznaczyć,
                że poprzedni workplace został przez niego opuszczony. */
                if (workerMap.getAction(Thread.currentThread().getId())
                        .equals(Action.SWITCHTO)) {
                    WorkplaceWrapper previousWorkplace =
                            workerMap.getCurrentWorkplace(Thread.currentThread().getId());
                    previousWorkplace.removeWorker(Thread.currentThread().getId());
                }

                addWorker(Thread.currentThread().getId());
                /* Nowa informacja o wątku, że chce zacząć pracę na stanowisku */
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

    public final static Workshop newWorkshop(Collection<Workplace> workplaces) {


        return new Workshop() {
            private final WorkersOccupyMap workerMap = new WorkersOccupyMap();
            private final Collection<WorkplaceWrapper> workplaceWrappers =
                    Utils.initializeWrappers(workplaces, workerMap);
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
                Set<Long> workers = workplace.getWorkers();
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
