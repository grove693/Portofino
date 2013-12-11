package com.manydesigns.portofino.tt

import com.manydesigns.elements.reflection.ClassAccessor
import com.manydesigns.elements.reflection.PropertyAccessor
import org.apache.commons.lang.ObjectUtils
import org.apache.shiro.SecurityUtils
import org.apache.shiro.subject.Subject
import org.hibernate.Session
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * User: predo
 * Date: 11/7/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
class TtUtils {

    public static final Logger logger = LoggerFactory.getLogger(TtUtils.class);

    static final public long TICKET_STATE_OPEN = 1L;
    static final public long TICKET_STATE_WORK_IN_PROGRESS = 2L;
    static final public long TICKET_STATE_RESOLVED = 3L;
    static final public long TICKET_STATE_CLOSED = 4L;

    static final public long ACTIVITY_TYPE_TICKET_CREATED = 1L;
    static final public long ACTIVITY_TYPE_TICKET_UPDATED = 2L;
    static final public long ACTIVITY_TYPE_TICKET_DELETED = 3L;
    static final public long ACTIVITY_TYPE_COMMENT_CREATED = 11L;
    static final public long ACTIVITY_TYPE_COMMENT_UPDATED = 12L;
    static final public long ACTIVITY_TYPE_COMMENT_DELETED = 13L;
    static final public long ACTIVITY_TYPE_ATTACHMENT_CREATED = 21L;
    static final public long ACTIVITY_TYPE_ATTACHMENT_UPDATED = 22L;
    static final public long ACTIVITY_TYPE_ATTACHMENT_DELETED = 23L;
    static final public long ACTIVITY_TYPE_AFFECTED_COMPONENT_CREATED = 31L;
    static final public long ACTIVITY_TYPE_AFFECTED_COMPONENT_UPDATED = 32L;
    static final public long ACTIVITY_TYPE_AFFECTED_COMPONENT_DELETED = 33L;

    static final public long ROLE_VIEWER = 1L;
    static final public long ROLE_CONTRIBUTOR = 2L;
    static final public long ROLE_EDITOR = 3L;
    static final public long ROLE_MANAGER = 4L;

    static public void addActivity(Session session, Object ticket,
                                   Long userId, Date date, long type,
                                   String message) {
        Map<String, Object> newActivity = new HashMap<String, Object>();
        newActivity.project = ticket.project;
        newActivity.n = ticket.n;
        newActivity.message = message;
        newActivity.type = type;
        newActivity.date = date;
        newActivity.user = userId;
        session.save("activity", (Object)newActivity);
    }

    static public String createDiffMessage(ClassAccessor accessor, Object from, Object to) {
        StringBuilder sb = new StringBuilder();
        for (PropertyAccessor current : accessor.getProperties()) {
            Object fromValue = current.get(from);
            Object toValue = current.get(to);
            if (!ObjectUtils.equals(fromValue, toValue)) {
                String diff = "$current.name: $fromValue -> $toValue";
                sb.append(diff);
            }
        }
        if (sb.length() == 0) {
            return null;
        } else {
            return sb.toString();
        }
    }


    static public boolean principalHasProjectRole(Object project, long minimumRole) {
        logger.debug("principalHasProjectRole() - project id: {} - minimum role : {}", project.id, minimumRole)
        Subject subject = SecurityUtils.subject
        if (!subject.isAuthenticated()) {
            logger.debug("Subject not authenticated")
            return false;
        }
        long userId = subject.principal.id;
        for (Object member : project.fk_member_project) {
            def memberUser = member.user
            def memberRole = member.role
            logger.debug("Member user: {} - member role: {}", memberUser, memberRole)
            if (memberUser == userId && memberRole >= minimumRole) {
                logger.debug("Member found")
                return true;
            }
        }
        logger.debug("Member not found")
        return false;
    }
}