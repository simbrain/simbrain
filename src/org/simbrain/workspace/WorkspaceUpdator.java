package org.simbrain.workspace;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class WorkspaceUpdator {
    private final Workspace workspace;
    private final CouplingManager manager;
    private final ExecutorService service;
    
    public WorkspaceUpdator(final Workspace workspace, final CouplingManager manager, int threads) {
        this.workspace = workspace;
        this.manager = manager;
        this.service = Executors.newFixedThreadPool(threads, factory);
    }
    
    private final ThreadFactory factory = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            return new UpdateThread(runnable);
        }
    };
    
    private class UpdateThread extends Thread {
        UpdateThread(Runnable runnable) {
            super(runnable);
        }
        
        void setCurrentTask(ComponentUpdate update) {
            
        }
        
        void clearCurrentTask() {
            
        }
    }
    
//    private class UpdateExecutor extends ThreadPoolExecutor {
//        UpdateExecutor(int threads) {
//            super(threads, threads, 0L, TimeUnit.MILLISECONDS, 
//                new LinkedBlockingQueue<Runnable>());
//        }
//    }
    
//    public WorkspaceUpdator(final Workspace workspace, final CouplingManager manager, 
//            final WorkspaceUpdate update) {
//        this(workspace, manager, update, Executors.newSingleThreadExecutor());
//    }
    
//    public WorkspaceUpdator(final Workspace workspace, final CouplingManager manager) {
//        this(workspace, manager, null, Executors.newSingleThreadExecutor());
//    }
    
    void doUpdate() {
        List<? extends WorkspaceComponent<?>> components = workspace.getComponentList();
        
        CyclicBarrier barrier = new CyclicBarrier(components.size(), new Runnable() {
            public void run() {
                manager.updateAllCouplings();
            }
        });
        
        for (WorkspaceComponent<?> component : workspace.getComponentList()) {
            service.submit(new ComponentUpdate(component, barrier));
        }
    }
    
    class ComponentUpdate implements Runnable {
        private final WorkspaceComponent<?> component;
        private final CyclicBarrier barrier;
        
        ComponentUpdate(WorkspaceComponent<?> component, CyclicBarrier barrier) {
            this.component = component;
            this.barrier = barrier;
        }

        @Override
        public void run() {
            UpdateThread thread = (UpdateThread) Thread.currentThread();
            
            thread.setCurrentTask(this);
            
            component.update();
            
            thread.clearCurrentTask();
            
            try {
                barrier.await();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        
    }
    
//    class ComponentUpdate implements Callable<Object> {
//        private final WorkspaceComponent<?> component;
//        private final CyclicBarrier barrier;
//        
//        ComponentUpdate(WorkspaceComponent<?> component, CyclicBarrier barrier) {
//            this.component = component;
//            this.barrier = barrier;
//        }
//
//        public Object call() throws Exception {
//            component.update();
//            barrier.await();
//            
//            return null;
//        }
//    }
}
