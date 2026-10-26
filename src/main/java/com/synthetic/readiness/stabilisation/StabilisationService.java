package com.synthetic.readiness.stabilisation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class StabilisationService {
    @PersistenceContext EntityManager em;
    @Value("${demo.events.retention.max-records:500}") int maxStoredEvents;
    @Transactional public StabilisationException route(PaymentStatusEvent event) {
        java.util.List<StabilisationException> matches=em.createQuery("select x from StabilisationException x where x.eventId=:eventId",StabilisationException.class).setParameter("eventId",event.eventId()).getResultList();
        StabilisationException existing=matches.isEmpty()?null:matches.getFirst();
        if(existing!=null)return existing;
        long stored=em.createQuery("select count(x) from StabilisationException x",Long.class).getSingleResult();
        if(stored>=maxStoredEvents) {
            java.util.List<Long> oldest=em.createQuery("select x.id from StabilisationException x order by x.receivedAt asc,x.id asc",Long.class)
                .setMaxResults((int)(stored-maxStoredEvents+1)).getResultList();
            if(!oldest.isEmpty()) em.createQuery("delete from StabilisationException x where x.id in :ids").setParameter("ids",oldest).executeUpdate();
        }
        StabilisationException x=new StabilisationException();x.eventId=event.eventId();x.releaseId=event.releaseId();x.paymentReference=event.paymentReference();x.status=event.status().toUpperCase();
        x.severity="REJECTED".equals(x.status)?"HIGH":"MEDIUM";x.occurredAt=event.occurredAt();x.receivedAt=LocalDateTime.now();x.journeyStep=event.journeyStep();
        x.owner="Nila Roy (synthetic)";x.nextAction="Review payment status and confirm the synthetic client update.";em.persist(x);em.flush();return x;
    }
    public java.util.List<StabilisationException> open(long releaseId){return em.createQuery("select x from StabilisationException x where x.releaseId=:id and x.state='OPEN' order by x.receivedAt desc",StabilisationException.class).setParameter("id",releaseId).getResultList();}
}

