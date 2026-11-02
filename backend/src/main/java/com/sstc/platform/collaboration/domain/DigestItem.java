package com.sstc.platform.collaboration.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * S03 - Notification Service: one ranked entry of the smart digest (US06).
 *
 * <p>Functionality mapping: {@code TeamCollaboration.SmartDigestEngine}.</p>
 */
@Entity
@Table(name = "tc_digest_item")
public class DigestItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "rank_score", nullable = false)
    private double rankScore = 0d;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "bucket", nullable = false, length = 24)
    private String bucket = "NORMAL";

    @Column(name = "dismissed", nullable = false)
    private boolean dismissed = false;

    public DigestItem() {
    }

    public DigestItem(Long userId, Long notificationId, double rankScore, String reason, String bucket) {
        this.userId = userId;
        this.notificationId = notificationId;
        this.rankScore = rankScore;
        this.reason = reason;
        this.bucket = bucket;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public double getRankScore() {
        return rankScore;
    }

    public void setRankScore(double rankScore) {
        this.rankScore = rankScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public boolean isDismissed() {
        return dismissed;
    }

    public void setDismissed(boolean dismissed) {
        this.dismissed = dismissed;
    }
}
