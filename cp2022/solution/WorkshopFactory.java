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

    public static Collection<WorkplaceWrapper> initializeWrappers(Collection<Workplace> workplaces) {
        return workplaces.stream().map(WorkplaceWrapper::new).collect(Collectors.toList());
    }

    public final static Workshop newWorkshop(Collection<Workplace> workplaces) {

        Collection<WorkplaceWrapper> workplaceWrappers = initializeWrappers(workplaces);


        HashMap<WorkplaceId, Semaphore> workplacesBlocker = initializeMap(workplaces);
        HashMap<Long, WorkplaceId> workplacesOccupyInfo = new HashMap<>();
        return new Workshop() {
            @Override
            public Workplace enter(WorkplaceId wid) {
//                try {
//                    workplacesBlocker.get(wid).acquire();
//                    Utils.occupyMap(workplacesOccupyInfo,
//                            Thread.currentThread().getId(), wid);
//
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt(); //todo change
//                }
//                sekcja krytyczna
                return getWorkplace(workplaceWrappers, wid); //todo change
            }

            @Override
            public Workplace switchTo(WorkplaceId wid) {
//                WorkplaceId currentWid = Utils.getOccupiedId(
//                        workplacesOccupyInfo, Thread.currentThread().getId());
//                if (currentWid.equals(wid)) {
//                    return Utils.getWorkplace(workplaces, wid);
//                }

                return getWorkplace(workplaceWrappers, wid); //todo change
            }

            @Override
            public void leave() {
//                Long pid = Thread.currentThread().getId();
//                WorkplaceId widToFree = Utils.getOccupiedId(workplacesOccupyInfo, pid);
//                Utils.freeMap(workplacesOccupyInfo, pid);
//                workplacesBlocker.get(widToFree).release();
            }
        };
    }

    public static class WorkplaceWrapper extends Workplace {
        private final Workplace wp;

        protected WorkplaceWrapper(Workplace wp) {
            super(wp.getId());
            this.wp = wp;
        }

        @Override
        public void use() {
            System.out.println("wejście do use");
            wp.use();
            System.out.println("wyjście z use");
        }
    }
}
