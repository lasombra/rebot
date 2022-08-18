/*
  The MIT License (MIT)

  Copyright (c) 2017 Rebasing.xyz ReBot

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal in
  the Software without restriction, including without limitation the rights to
  use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
  the Software, and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package xyz.rebasing.rebot.service.persistence.repository;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import org.jboss.logging.Logger;
import xyz.rebasing.rebot.service.persistence.domain.Notion;

@Transactional
@ApplicationScoped
public class NotionRepository {

    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @Inject
    EntityManager em;

    public int get(String key) {
        try {
            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<String> notionPoints = criteriaBuilder.createQuery(String.class);
            Root<Notion> notion = notionPoints.from(Notion.class);
            notionPoints.select(notion.get("points")).where(criteriaBuilder.equal(notion.get("username"), key));
            return Integer.parseInt(em.createQuery(notionPoints).getSingleResult());
        } catch (final Exception e) {
            log.debugv("get() - There is no notion for [{0}]", key);
            return 0;
        }
    }

    public List<Notion> list(String key) {
        try {
            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<Notion> notionQuery = criteriaBuilder.createQuery(Notion.class);
            Root<Notion> notion = notionQuery.from(Notion.class);
            notionQuery.select(notion)
                    .where(criteriaBuilder.like(notion.get("username"), key))
                    .orderBy(criteriaBuilder.asc(notion.get("username")));
            return em.createQuery(notionQuery).getResultList();
        } catch (final Exception e) {
            log.debugv("list() - There is no notion for [{0}]", key);
            return Arrays.asList(new Notion(key, "0"));
        }
    }

    public void updateOrCreateNotion(Notion notion) {
        log.debugv("Updating the notion for [{0}]", notion.toString());
        try {
            em.merge(notion);
            em.flush();
        } catch (final Exception e) {
            log.warnv("Failed to persist object [{0}]: {1}", notion.toString(), e.getMessage());
        }
    }
}
