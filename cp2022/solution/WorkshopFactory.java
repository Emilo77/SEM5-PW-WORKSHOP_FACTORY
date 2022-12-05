/*
 * University of Warsaw
 * Concurrent Programming Course 2022/2023
 * Java Assignment
 *
 * Author: Konrad Iwanicki (iwanicki@mimuw.edu.pl)
 */
package cp2022.solution;

import java.util.Collection;

import cp2022.base.Workplace;
import cp2022.base.Workshop;
import cp2022.base.WorkplaceId;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static cp2022.solution.Utils.Action;
import static cp2022.solution.Utils.getWorkplace;


public final class WorkshopFactory {
    static String runtimeMessage = "panic: unexpected thread interruption";

    /* Klasa pomocnicza, będąca wrapperem dla stanowiska. */
    static class WorkplaceWrapper extends Workplace {
        private final Workplace workplace;
        private final Semaphore enterMutex;
        private final Semaphore workingMutex;
        private final WorkersOccupyMap workerMap;
        private final Set<Long> workers;
        private final Semaphore setBlocker;


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

                /* Dodajemy informację o sobie na stanowisku. */
                addWorker(Thread.currentThread().getId());
                /* Nowa informacja o wątku, że chce zacząć pracę na stanowisku */
                workerMap.updateWorkerInfo(
                        Thread.currentThread().getId(),
                        Action.USE,
                        this);

                /* Podniesienie semafora na pracę,
                czekamy, aż otrzymamy pozwolenie, że możemy pracować. */
                workingMutex.acquire();

                /* Rozpoczynamy pracę na stanowisku. */
                workplace.use();
            } catch (InterruptedException e) {
                throw new RuntimeException(runtimeMessage);
            }
        }
    }

    public final static Workshop newWorkshop(Collection<Workplace> workplaces) {
        final WorkersOccupyMap workerMap = new WorkersOccupyMap();
        final Collection<WorkplaceWrapper> workplaceWrappers =
                Utils.initializeWrappers(workplaces, workerMap);
        final WorkersWaitingQueue waitingQueue =
                new WorkersWaitingQueue(workplaces.size());
        return new Workshop() {


            @Override
            public Workplace enter(WorkplaceId wid) {
                try {

                    /* Dodanie wątku do kolejki wejściowej. */
                    waitingQueue.add(Thread.currentThread().getId());

                    /* Dodanie wątku do mapy z informacjami. */
                    workerMap.insertNewWorker(Thread.currentThread().getId());

                    /* Wyszukanie stanowiska o podanym identyfikatorze. */
                    WorkplaceWrapper workplace = getWorkplace(workplaceWrappers, wid);

                    /* Nowa informacja o wątku, że chce wejść na stanowisko. */
                    workerMap.updateWorkerInfo(
                            Thread.currentThread().getId(),
                            Action.ENTER,
                            workplace);

                    /* Zawieszenie się na wejściu na stanowisko. */
                    workplace.enterMutex.acquire();

                    /* Przejście przez kolejkę innych czekających wątków. */
                    waitingQueue.pass(Thread.currentThread().getId());
                    waitingQueue.remove(Thread.currentThread().getId());

                    return workplace;
                } catch (InterruptedException e) {
                    throw new RuntimeException(runtimeMessage);
                }
            }

            @Override
            //todo sytuacja, kiedy robimy switchTo na to samo stanowisko
            public Workplace switchTo(WorkplaceId wid) {
                try {

                    /* Zaznaczenie, że czeka się w kolejce do chęci użycia. */
                    waitingQueue.add(Thread.currentThread().getId());

                    /* Wyszukanie aktualnego stanowiska. */
                    WorkplaceWrapper currentWorkplace =
                            workerMap.getCurrentWorkplace(Thread.currentThread().getId());

                    /* Wyszukanie następnego stanowiska. */
                    WorkplaceWrapper futureWorkplace = getWorkplace(workplaceWrappers, wid);

                    /* Jeżeli worker chce zmienić stanowisko na takie samo,
                     * posiada priorytet i od razu może ponownie wykonać USE. */
                    if (currentWorkplace.getId() == futureWorkplace.getId()) {
                        /* Nowa informacja, że wątek chce wykonać USE. */
                        workerMap.updateWorkerInfo(
                                Thread.currentThread().getId(),
                                Action.USE,
                                futureWorkplace);
                        /* Usunięcie się z kolejki, ponieważ mamy priorytet. */
                        waitingQueue.remove(Thread.currentThread().getId());

                        /* Zwolnienie ochrony do użycia USE, możemy tak zrobić,
                        * ponieważ wiemy, że będziemy sami na stanowisku. */
                        currentWorkplace.workingMutex.release();
                        return futureWorkplace;
                    }

                    /* Nowa informacja o wątku, że chce zmienić stanowisko. */
                    workerMap.updateWorkerInfo(
                            Thread.currentThread().getId(),
                            Action.SWITCHTO,
                            currentWorkplace,
                            futureWorkplace);


                    /* Podniesienie semafora,
                    inny wątek może teraz wejść na nasze aktualne stanowisko. */
                    currentWorkplace.enterMutex.release();

                    /* Opuszczenie semafora, czekamy, aż dostaniemy pozwolenie
                    * na wejście do nowego stanowiska. */
                    futureWorkplace.enterMutex.acquire();

                    /* Jeżeli możemy już wejść,
                    * usuwamy się z kolejki wątków oczekujących. */
                    waitingQueue.remove(Thread.currentThread().getId());

                    /* Zezwalamy na pracę innego wątku na aktualnym stanowisku. */
                    currentWorkplace.workingMutex.release();

                    return futureWorkplace;

                } catch (InterruptedException e) {
                    throw new RuntimeException(runtimeMessage);
                }
            }

            @Override
            public void leave() {
                try {
                    /* Wyszukanie aktualnego stanowiska. */
                    WorkplaceWrapper workplace = workerMap.getCurrentWorkplace(
                            Thread.currentThread().getId());

                    /* Zaznaczamy, że wychodzimy ze stanowiska. */
                    workplace.workers.remove(Thread.currentThread().getId());
                    workerMap.removeWorker(Thread.currentThread().getId());

                    /* Opuszczamy oba semafory na wejście i pracę
                    * na opuszczonym przez nas stanowisku,
                    * inny wątek może wejść i zacząć pracę. */
                    workplace.enterMutex.release();
                    workplace.workingMutex.release();
                } catch (InterruptedException e) {
                    throw new RuntimeException(runtimeMessage);
                }

            }
        };
    }
}
