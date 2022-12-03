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
        HashMap<Long, WorkplaceWrapper> occupyMap;

        protected WorkersOccupyMap(Collection<WorkplaceWrapper> workplaceWrappers) {
            this.occupyMap = new HashMap<>();
            this.workplaceWrappers = workplaceWrappers;
        }

        protected void put(WorkplaceId wid) {
            long pid = Thread.currentThread().getId();
            WorkplaceWrapper wp = Utils.getWorkplace(workplaceWrappers, wid);
            occupyMap.put(pid, wp); //sprawdzenie, czy elementu nie ma już w mapie
        }

        protected void free() {
            long pid = Thread.currentThread().getId();
            if (occupyMap.get(pid) == null) {
                throw new RuntimeException("panic: couldn't find workplace occupied by this pid!");
            }
            occupyMap.remove(pid);
        }

        protected WorkplaceWrapper get() {
            long pid = Thread.currentThread().getId();
            if (occupyMap.get(pid) == null) {
                throw new RuntimeException("panic: couldn't find workplace occupied by this pid!");
            }
            return occupyMap.get(pid);
        }

    }

    public static Collection<WorkplaceWrapper> initializeWrappers(Collection<Workplace> workplaces) {
        return workplaces.stream().map(WorkplaceWrapper::new).collect(Collectors.toList());
    }

    public static class WorkplaceWrapper extends Workplace {
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
            } finally {
                enterMutex.release();
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

                    return workplace.wp;
                } catch (InterruptedException e) {
                    throw new RuntimeException(runtimeMessage);
                }
            }

            @Override //todo sytuacja, kiedy robimy switchTo na to samo stanowisko
            public Workplace switchTo(WorkplaceId wid) {
                try {
                    WorkplaceWrapper pastWorkplace = occupyMap.get();
                    WorkplaceWrapper futureWorkplace = getWorkplace(workplaceWrappers, wid);

                    futureWorkplace.enterMutex.acquire();
                    occupyMap.free();
                    occupyMap.put(wid);
                    pastWorkplace.workingMutex.release();

                    return futureWorkplace.wp;

                } catch (InterruptedException e) {
                    throw new RuntimeException(runtimeMessage);
                }
            }

            @Override
            public void leave() {
                WorkplaceWrapper workplace = occupyMap.get();
                occupyMap.free();
                workplace.workingMutex.release();
            }
        };
    }
}
