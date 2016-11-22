package com.appdirect.subscriptions.operations.createsubscription;

import com.appdirect.subscriptions.notifications.DBRepository;
import com.appdirect.subscriptions.notifications.domain.NotificationType;
import com.appdirect.subscriptions.notifications.domain.SubscriptionNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by hrishikeshshinde on 22/11/16.
 */

@Component
public class EventProcessor {

    private static final Logger log = LoggerFactory.getLogger(EventProcessor.class);

    @Autowired
    private DBRepository notificationRepo;

    @Scheduled(fixedRate = 10000)
    public void processEvents() {
        ExecutorService eventService = getExecutorService();
        List<SubscriptionNotification> notifications = getSubscriptionNotifications();
        processSubscriptions(eventService, notifications);
        waitFortermintation(eventService);
    }

    private void waitFortermintation(ExecutorService eventService) {
        eventService.shutdown();
        try {
            eventService.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Create event processor await : " + e.getMessage());
        }
    }

    private void processSubscriptions(ExecutorService eventService, List<SubscriptionNotification> notifications) {
        notifications.forEach(n -> {
            log.debug("Subscription Event Id : " + n.getId());
            n.setProcessed(true);
            notificationRepo.save(n);
            eventService.submit(new EventWorker(n, notificationRepo));
        });
    }

    private List<SubscriptionNotification> getSubscriptionNotifications() {
        return (List<SubscriptionNotification>)
                    notificationRepo.findByTypeAndStatus(NotificationType.CREATE, false);
    }

    private ExecutorService getExecutorService() {
        int noOfProcess = Runtime.getRuntime().availableProcessors();
        return Executors.newFixedThreadPool(noOfProcess);
    }
}
