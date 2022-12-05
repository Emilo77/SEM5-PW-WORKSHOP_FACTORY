package cp2022.solution;

import java.util.concurrent.Semaphore;

public class WorkerInfo {

    private final String name; //do usunięcia później
    private final Long pid;
    private Utils.Action desiredAction;
    private WorkshopFactory.WorkplaceWrapper currentWorkplace;
    private WorkshopFactory.WorkplaceWrapper desiredWorkplace;
    private Semaphore mutex;

    public WorkerInfo() {
        this.name = Thread.currentThread().getName();
        this.pid = Thread.currentThread().getId();
        this.desiredAction = Utils.Action.ENTER;
        this.currentWorkplace = null;
        this.desiredWorkplace = null;
        this.mutex = new Semaphore(1, true);
    }

    public void update(Utils.Action newAction,
                       WorkshopFactory.WorkplaceWrapper workplace) {

        this.desiredAction = newAction;

        if (newAction.equals(Utils.Action.ENTER)) {
            this.currentWorkplace = null;
            this.desiredWorkplace = workplace;
        } else if (newAction.equals(Utils.Action.USE)) {
            this.currentWorkplace = workplace;
            this.desiredWorkplace = null;
        }
    }

    public void update(Utils.Action newAction,
                       WorkshopFactory.WorkplaceWrapper newCurrentWorkplace,
                       WorkshopFactory.WorkplaceWrapper newDesiredWorkplace) {

        desiredAction = newAction;
        currentWorkplace = newCurrentWorkplace;
        desiredWorkplace = newDesiredWorkplace;
    }

    public WorkshopFactory.WorkplaceWrapper getCurrentWorkplace() {
        return this.currentWorkplace;
    }

    public WorkshopFactory.WorkplaceWrapper getDesiredWorkplace() {
        return this.desiredWorkplace;
    }

    public Utils.Action getAction() {
        return this.desiredAction;
    }
}