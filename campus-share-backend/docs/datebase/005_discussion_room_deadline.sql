-- 在001至004之后执行。讨论室不套用座位/教室/体育的签到期限。
USE campusshare;
ALTER TABLE reservation MODIFY COLUMN arrival_deadline DATETIME(3) NULL;
UPDATE reservation r JOIN resource s ON s.id=r.resource_id
SET r.arrival_deadline=NULL WHERE s.resource_type='DISCUSSION_ROOM';
