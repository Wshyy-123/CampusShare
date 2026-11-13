-- 007：时段维护留存修改前后快照；删除当前安排不删除教学历史。
USE campusshare;

CREATE TABLE resource_schedule_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    schedule_type VARCHAR(16) NOT NULL,
    schedule_id BIGINT NOT NULL,
    operation VARCHAR(16) NOT NULL,
    operator_id BIGINT NOT NULL,
    occurred_at DATETIME(3) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    before_json LONGTEXT NULL,
    after_json LONGTEXT NULL,
    CONSTRAINT fk_schedule_event_resource FOREIGN KEY(resource_id) REFERENCES resource(id),
    CONSTRAINT fk_schedule_event_operator FOREIGN KEY(operator_id) REFERENCES app_user(id)
);
