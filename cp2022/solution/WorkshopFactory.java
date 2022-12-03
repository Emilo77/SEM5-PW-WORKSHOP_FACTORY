/*
 * University of Warsaw
 * Concurrent Programming Course 2022/2023
 * Java Assignment
 *
 * Author: Konrad Iwanicki (iwanicki@mimuw.edu.pl)
 */
package cp2022.solution;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;

import static cp2022.solution.Utils.*;


public final class WorkshopFactory {
    static String runtimeMessage = "panic: unexpected thread interruption";

    public static class WorkersOccupyMap {
        Collection<WorkplaceWrapper> workplaceWrappers;
        ConcurrentHashMap<Long, WorkplaceWrapper> occupyMap;

        Semaphore mutex;

        protected WorkersOccupyMap(Collection<WorkplaceWrapper> workplaceWrappers) {
            this.occupyMap = new ConcurrentHashMap<>();
            this.workplaceWrappers = workplaceWrappers;
            this.mutex = new Semaphore(1, true);
        }

        protected void put(WorkplaceId wid) {
            occupyMap.put(Thread.currentThread().getId(),
                    Utils.getWorkplace(workplaceWrappers, wid)); //sprawdzenie, czy elementu nie ma już w mapie
        }

        protected void free() {
            if (occupyMap.get(Thread.currentThread().getId()) == null) {
                throw new RuntimeException("panic: couldn't find workplace occupied by this pid! free");
            }
            occupyMap.remove(Thread.currentThread().getId());
        }

        protected WorkplaceWrapper get() {
            if (occupyMap.get(Thread.currentThread().getId()) == null) {
                throw new RuntimeException("panic: couldn't find workplace occupied by this pid! get");
            }
            return occupyMap.get(Thread.currentThread().getId());
        }

    }

    public static Collection<WorkplaceWrapper> initializeWrappers(Collection<Workplace> workplaces) {
        return workplaces.stream().map(WorkplaceWrapper::new).collect(Collectors.toList());
    }

    static class WorkplaceWrapper extends Workplace {
        private final Workplace wp;
        private final Semaphore enterMutex;
        private final Semaphore workingMutex;

        protected WorkplaceWrapper(Workplace wp) {
            super(wp.getId());
            this.wp = wp;
            this.enterMutex = new Semaphore(1, true);
            this.workingMutex = new Semaphore(1, true);
        }

        @Override
        public void use() {
            try {
                workingMutex.acquire();
                wp.use();
            } catch (InterruptedException e) {
                throw new RuntimeException(runtimeMessage);
            }
        }
    }

    public final static Workshop newWorkshop(Collection<Workplace> workplaces) {

        Collection<WorkplaceWrapper> workplaceWrappers = initializeWrappers(workplaces);
        WorkersOccupyMap occupyMap = new WorkersOccupyMap(workplaceWrappers);
        return new Workshop() {
            @Override
            public Workplace enter(WorkplaceId wid) {
                try {
                    WorkplaceWrapper workplace = getWorkplace(workplaceWrappers, wid);
                    workplace.enterMutex.acquire();
                    occupyMap.put(wid);

                    return workplace;
                } catch (InterruptedException e) {
                    throw new RuntimeException(runtimeMessage);
                }
            }

            @Override //todo sytuacja, kiedy robimy switchTo na to samo stanowisko
            public Workplace switchTo(WorkplaceId wid) {
                try {
                    WorkplaceWrapper pastWorkplace = occupyMap.get();
                    WorkplaceWrapper futureWorkplace = getWorkplace(workplaceWrappers, wid);

                    if (pastWorkplace.getId() == futureWorkplace.getId()) {
                        pastWorkplace.workingMutex.release();
                        return futureWorkplace;
                    }

                    // w tym miejscu trzeba jakoś wykrywać cykle i nie wpuszczać
                    // innych pracowników na stanowiska będące w cyklu
                    pastWorkplace.enterMutex.release();
                    futureWorkplace.enterMutex.acquire();

                    occupyMap.free();
                    occupyMap.put(wid);

                    pastWorkplace.workingMutex.release();

                    return futureWorkplace;

                } catch (InterruptedException e) {
                    throw new RuntimeException(runtimeMessage);
                }
            }

            @Override
            public void leave() {
                WorkplaceWrapper workplace = occupyMap.get();
                occupyMap.free();
                workplace.enterMutex.release();
                workplace.workingMutex.release();
            }
        };
    }
}
