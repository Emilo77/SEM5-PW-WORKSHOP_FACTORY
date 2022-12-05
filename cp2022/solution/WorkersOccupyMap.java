package cp2022.solution;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class WorkersOccupyMap {
    ConcurrentHashMap<Long, WorkerInfo> occupyMap;

    Semaphore mapBlocker;

    protected WorkersOccupyMap() {
        this.occupyMap = new ConcurrentHashMap<>();
        this.mapBlocker = new Semaphore(1, true);
    }

    protected void insertNewWorker(Long pid) throws InterruptedException {
        mapBlocker.acquire();

        WorkerInfo info = new WorkerInfo();
        occupyMap.put(pid, info);

        mapBlocker.release();
    }

    protected void removeWorker(Long pid) throws InterruptedException {
        mapBlocker.acquire();

        WorkerInfo info = occupyMap.get(pid);

        if (info == null) {
            throw new RuntimeException("panic: couldn't find workplace occupied by this pid! free");
        }
        occupyMap.remove(pid);

        mapBlocker.release();
    }

    protected WorkshopFactory.WorkplaceWrapper getCurrentWorkplace(Long pid) throws InterruptedException {
        mapBlocker.acquire();

        WorkerInfo info = occupyMap.get(pid);
        if (info == null) {
            throw new RuntimeException("Map panic: getCurrentWorkplace: couldn't find workplace occupied by this pid!");
        }

        mapBlocker.release();
        return info.getCurrentWorkplace(); //tu może być problem, że jest po release
    }

    protected WorkshopFactory.WorkplaceWrapper getDesiredWorkplace(Long pid) throws InterruptedException {
        mapBlocker.acquire();

        WorkerInfo info = occupyMap.get(pid);
        if (info == null) {
            throw new RuntimeException("Map panic: getDesiredWorkplace: couldn't find workplace occupied by this pid! get");
        }

        mapBlocker.release();
        return info.getDesiredWorkplace();
    }

    protected Utils.Action getAction(Long pid) throws InterruptedException {
        mapBlocker.acquire();

        WorkerInfo info = occupyMap.get(pid);
        if (info == null) {
            throw new RuntimeException("Map panic: getDesiredWorkplace: couldn't find workplace occupied by this pid! get");
        }

        mapBlocker.release();
        return info.getAction();
    }

    protected void updateWorkerInfo(Long pid,
                                    Utils.Action action,
                                    WorkshopFactory.WorkplaceWrapper workplace)
            throws InterruptedException {
        mapBlocker.acquire();

        WorkerInfo info = occupyMap.get(pid);
        if (info == null) {
            throw new RuntimeException("Map panic: getDesiredWorkplace: couldn't find workplace occupied by this pid! get");
        }

        info.update(action, workplace);

        mapBlocker.release();
    }

    protected void updateWorkerInfo(Long pid,
                                    Utils.Action action,
                                    WorkshopFactory.WorkplaceWrapper currentWorkplace,
                                    WorkshopFactory.WorkplaceWrapper desiredWorkplace)
            throws InterruptedException {
        mapBlocker.acquire();

        WorkerInfo info = occupyMap.get(pid);
        if (info == null) {
            throw new RuntimeException("Map panic: getDesiredWorkplace: couldn't find workplace occupied by this pid! get");
        }

        info.update(action, currentWorkplace, desiredWorkplace);

        mapBlocker.release();
    }

}