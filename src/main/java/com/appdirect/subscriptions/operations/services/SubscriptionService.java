package com.appdirect.subscriptions.operations.services;

import com.appdirect.common.exceptions.AuthException;
import com.appdirect.common.exceptions.EntityNotFoundException;
import com.appdirect.common.services.OAuthHelper;
import com.appdirect.subscriptions.operations.domain.entities.Account;
import com.appdirect.subscriptions.operations.domain.entities.AccountStatusEnum;
import com.appdirect.subscriptions.operations.domain.entities.Subscription;
import com.appdirect.subscriptions.operations.domain.repos.SubscriptionRepo;
import com.appdirect.subscriptions.operations.exceptions.ServiceException;
import oauth.signpost.exception.OAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Created by hrishikeshshinde on 22/11/16.
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);


    @Autowired
    private OAuthHelper oAuthHelper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AccountService accountService;

    @Autowired
    private SubscriptionRepo subscriptionRepo;

    public Subscription getByEventUrl(String url) throws ServiceException {
        try {
            String signedURL = oAuthHelper.signURL(url);
            log.info("Signed URL : " +  signedURL);
            return restTemplate.getForObject(signedURL, Subscription.class);
        } catch (OAuthException e) {
            throw new AuthException(e);
        } catch (RestClientException e) {
            if(e.getMessage().equalsIgnoreCase(AuthException.UNAUTHORIZED)) {
                throw new AuthException(e);
            } else {
                throw new ServiceException(e);
            }
        }
    }

    public Subscription create(Subscription subscription) throws DataIntegrityViolationException {
        Account account = accountService.create(new Account(AccountStatusEnum.ACTIVE));
        subscription.getPayload().setAccount(account);
        return subscriptionRepo.save(subscription);
    }

    public Subscription update(Subscription subscription) throws EntityNotFoundException, DataIntegrityViolationException {
        String accountId = subscription.getPayload().getAccount().getAccountIdentifier();
        Subscription currentSubscription;
        try{
            currentSubscription = subscriptionRepo.findByPayloadAccountAccountIdentifier(accountId);
        }catch(Exception e){
            throw new EntityNotFoundException("Subscription for account "+accountId+" not found", e);
        }

        try{
            subscription.setId(currentSubscription.getId());
            subscriptionRepo.save(subscription);
        }catch(Exception e){
            throw new DataIntegrityViolationException("Subscription update error with account "+accountId, e);
        }

        return subscription;
    }

    public Subscription cancel(Subscription subscription) throws EntityNotFoundException {

        String accountId = subscription.getPayload().getAccount().getAccountIdentifier();

        try {
            Account account = accountService.findByAccountId(accountId);
            account.setStatus(AccountStatusEnum.INACTIVE);
            accountService.updateAccount(account);
            subscription.getPayload().setAccount(account);
            return subscription;
        } catch(Exception e){
            throw new EntityNotFoundException("Account not found", e);
        }
    }
}
