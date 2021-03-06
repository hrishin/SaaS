package com.appdirect.subscriptions.notifications;

import com.appdirect.common.domain.ServiceResponse;
import com.appdirect.common.services.OAuthHelper;
import com.appdirect.subscriptions.notifications.domain.NotificationType;
import com.appdirect.subscriptions.notifications.domain.SubscriptionNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Created by hrishikeshshinde on 21/11/16.
 *
 * Rest controller to handel subscription events from AppDirect
 */

@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OAuthHelper oAuthHelper;

    /**
     * Endpoint for handling create, update, cancel events
     *
     * @param type String
     * @param eventUrl String
     * @param authorization String
     * @return returns ServiceResponse entity
     */
    @RequestMapping(path = "/{type}/subscriptions", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse> subscribeNotification(@PathVariable String type,
                                                                 @RequestParam(value = "eventUrl", required = true) String eventUrl,
                                                                 @RequestHeader(value = "Authorization") String authorization) {
        LOGGER.debug("Auth : " + authorization);
        oAuthHelper.authenticateSignature(authorization);

        notificationService.createNewEvent(new SubscriptionNotification(eventUrl, NotificationType.valueOf(type), false));
        return new ResponseEntity<ServiceResponse>(new ServiceResponse(true), HttpStatus.ACCEPTED);
    }
}
